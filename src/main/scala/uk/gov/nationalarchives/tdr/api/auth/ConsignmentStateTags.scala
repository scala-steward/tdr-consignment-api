package uk.gov.nationalarchives.tdr.api.auth

import java.util.UUID

import sangria.execution.{BeforeFieldResult, FieldTag}
import sangria.schema.{Argument, Context}
import uk.gov.nationalarchives.tdr.api.graphql.ConsignmentApiContext
import uk.gov.nationalarchives.tdr.api.graphql.validation.UserOwnsConsignment

import scala.concurrent.{ExecutionContext, Future}

trait ConsignmentStateTag extends FieldTag {
  def validate(ctx: Context[ConsignmentApiContext, _])
              (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]]

  val continue: BeforeFieldResult[ConsignmentApiContext, Unit] = BeforeFieldResult(())
}

case class ValidateNoPreviousUploadForConsignment[T](argument: Argument[T]) extends ConsignmentStateTag {
  override def validate(ctx: Context[ConsignmentApiContext, _])
                       (implicit executionContext: ExecutionContext): Future[BeforeFieldResult[ConsignmentApiContext, Unit]] = {
    val arg: T = ctx.arg[T](argument.name)
    val consignmentId: UUID = arg match {
      case uoc: UserOwnsConsignment => uoc.consignmentId
      case id: UUID => id
    }

    ctx.ctx.consignmentService.consignmentHasFiles(consignmentId).map {
      case false => continue
      case true => throw ConsignmentStateException("Upload already occurred for consignment: " + consignmentId)
    }
  }
}

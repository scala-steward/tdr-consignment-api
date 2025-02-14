package uk.gov.nationalarchives.tdr.api.routes

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import com.dimafeng.testcontainers.PostgreSQLContainer
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.utils.TestContainerUtils._
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._
import uk.gov.nationalarchives.tdr.api.utils.TestAuthUtils._
import uk.gov.nationalarchives.tdr.api.utils.{TestContainerUtils, TestRequest, TestUtils}

import java.time.ZonedDateTime
import java.util.UUID


class ConsignmentStatusRouteSpec extends TestContainerUtils with Matchers with TestRequest {
  override def afterContainersStart(containers: containerDef.Container): Unit = super.afterContainersStart(containers)

  private val markUploadAsCompletedJsonFilePrefix: String = "json/updateconsignmentstatus_"
  val runTestMutation: (String, OAuth2BearerToken) => GraphqlMutationData =
    runTestRequest[GraphqlMutationData](markUploadAsCompletedJsonFilePrefix)
  val expectedMutationResponse: String => GraphqlMutationData =
    getDataFromFile[GraphqlMutationData](markUploadAsCompletedJsonFilePrefix)

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  case class GraphqlMutationData(data: Option[UpdateConsignmentStatusUploadComplete], errors: List[GraphqlError] = Nil)

  case class UpdateConsignmentStatusUploadComplete(updateConsignmentStatusUploadComplete: Option[Int])

  case class ConsignmentStatus(consignmentStatusId: Option[UUID],
                               consignmentId: Option[UUID],
                               statusType: Option[String],
                               value: Option[String],
                               createdDatetime: Option[ZonedDateTime],
                               modifiedDatetime: Option[ZonedDateTime]
                              )

  "setUploadConsignmentStatusValueToComplete" should "update consignment status" in withContainers {
    case container: PostgreSQLContainer =>
      val utils = TestUtils(container.database)
      val consignmentId = UUID.fromString("a8dc972d-58f9-4733-8bb2-4254b89a35f2")
      val userId = UUID.fromString("49762121-4425-4dc4-9194-98f72e04d52e")
      val statusType = "Upload"
      val statusValue = "InProgress"
      val token = validUserToken(userId)

      utils.createConsignment(consignmentId, userId)
      utils.createConsignmentStatus(consignmentId, statusType, statusValue)

      val expectedResponse = getDataFromFile[GraphqlMutationData](markUploadAsCompletedJsonFilePrefix)("data_all")
      val response = runTestRequest[GraphqlMutationData](markUploadAsCompletedJsonFilePrefix)("mutation_data_all", token)

      response.data.get.updateConsignmentStatusUploadComplete should equal(expectedResponse.data.get.updateConsignmentStatusUploadComplete)
  }

  "markUploadAsCompleted" should "not allow a user to update the consignment status of a consignment that they did not create" in withContainers {
    case container: PostgreSQLContainer =>
      val utils = TestUtils(container.database)
      val consignmentId = UUID.fromString("a8dc972d-58f9-4733-8bb2-4254b89a35f2")
      val userId = UUID.fromString("49762121-4425-4dc4-9194-98f72e04d52e")
      val statusType = "Upload"
      val statusValue = "InProgress"

      utils.createConsignment(consignmentId, userId)
      utils.createConsignmentStatus(consignmentId, statusType, statusValue)

      val wrongUserId = UUID.fromString("29f65c4e-0eb8-4719-afdb-ace1bcbae4b6")
      val token = validUserToken(wrongUserId)

      val expectedResponse = getDataFromFile[GraphqlMutationData](markUploadAsCompletedJsonFilePrefix)("data_not_owner")
      val response = runTestRequest[GraphqlMutationData](markUploadAsCompletedJsonFilePrefix)("mutation_not_owner", token)

      response.errors.head.message should equal(expectedResponse.errors.head.message)
  }

  "markUploadAsCompleted" should "return an error if a consignment that doesn't exist is queried" in withContainers {
    case _: PostgreSQLContainer =>
      val userId = UUID.fromString("dfee3d4f-3bb1-492e-9c85-7db1685ab12f")
      val token = validUserToken(userId)

      val expectedResponse = getDataFromFile[GraphqlMutationData](markUploadAsCompletedJsonFilePrefix)("data_invalid_consignmentid")
      val response = runTestRequest[GraphqlMutationData](markUploadAsCompletedJsonFilePrefix)("mutation_invalid_consignmentid", token)

      response.errors.head.message should equal(expectedResponse.errors.head.message)
  }
}

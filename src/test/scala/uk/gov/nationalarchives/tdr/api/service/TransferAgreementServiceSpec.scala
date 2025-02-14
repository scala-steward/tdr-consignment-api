package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.util.UUID
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables._
import uk.gov.nationalarchives.tdr.api.db.repository.{ConsignmentMetadataRepository, ConsignmentStatusRepository}
import uk.gov.nationalarchives.tdr.api.graphql.fields.TransferAgreementFields.{
  AddTransferAgreementComplianceInput,
  AddTransferAgreementPrivateBetaInput,
  TransferAgreementCompliance,
  TransferAgreementPrivateBeta
}
import uk.gov.nationalarchives.tdr.api.utils.{FixedTimeSource, FixedUUIDSource}

import java.time.Instant.now
import scala.concurrent.{ExecutionContext, Future}

class TransferAgreementServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  val fixedUuidSource = new FixedUUIDSource()
  val fixedTimeSource: FixedTimeSource.type = FixedTimeSource

  "addTransferAgreementPrivateBeta" should "add the correct metadata given correct arguments and set TA status to InProgress" in {

    val consignmentMetadataRepositoryMock = mock[ConsignmentMetadataRepository]
    val consignmentStatusRepositoryMock = mock[ConsignmentStatusRepository]
    val metadataId = UUID.randomUUID()
    val consignmentId = UUID.randomUUID()
    val consignmentStatusId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    val dateTime = Timestamp.from(FixedTimeSource.now)
    def row(name: String, value: String): ConsignmentmetadataRow =
      ConsignmentmetadataRow(metadataId, consignmentId, name, value, dateTime, userId)
    val mockResponse = Future.successful(Seq(
      row("AllEnglishConfirmed", "true"),
      row("CrownCopyrightConfirmed", "true"),
      row("PublicRecordsConfirmed", "true")
    ))
    val statusType = "TransferAgreement"
    val statusValue = "InProgress"

    val mockTaConsignmentStatus = ConsignmentstatusRow(consignmentStatusId, consignmentId, statusType, statusValue, dateTime, None)

    when(consignmentMetadataRepositoryMock.addConsignmentMetadata(any[Seq[ConsignmentmetadataRow]])).thenReturn(mockResponse)
    when(consignmentStatusRepositoryMock.addConsignmentStatus(any[ConsignmentstatusRow])).thenReturn(Future.successful(mockTaConsignmentStatus))

    val service = new TransferAgreementService(consignmentMetadataRepositoryMock, consignmentStatusRepositoryMock, fixedUuidSource, fixedTimeSource)
    val transferAgreementResult: TransferAgreementPrivateBeta = service.addTransferAgreementPrivateBeta(
      AddTransferAgreementPrivateBetaInput(
        consignmentId,
        allCrownCopyright = true,
        allEnglish = true,
        allPublicRecords = true),
      userId
    ).futureValue

    transferAgreementResult.consignmentId shouldBe consignmentId
    transferAgreementResult.allCrownCopyright shouldBe true
    transferAgreementResult.allEnglish shouldBe true
    transferAgreementResult.allPublicRecords shouldBe true
  }

  "addTransferAgreementCompliance" should "add the correct metadata given correct arguments and set TA status to Completed" in {
    val consignmentMetadataRepositoryMock = mock[ConsignmentMetadataRepository]
    val consignmentStatusRepositoryMock = mock[ConsignmentStatusRepository]
    val metadataId = UUID.randomUUID()
    val consignmentId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    val dateTime = Timestamp.from(FixedTimeSource.now)
    val statusType = "TransferAgreement"
    def row(name: String, value: String): ConsignmentmetadataRow =
      ConsignmentmetadataRow(metadataId, consignmentId, name, value, dateTime, userId)
    val mockResponse = Future.successful(Seq(
      row("AppraisalSelectionSignOffConfirmed", "true"),
      row("InitialOpenRecordsConfirmed", "true"),
      row("SensitivityReviewSignOffConfirmed", "true")
    ))
    val transferAgreementStatusTypeCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val transferAgreementStatusValueCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val statusValue = "Completed"

    when(consignmentMetadataRepositoryMock.addConsignmentMetadata(any[Seq[ConsignmentmetadataRow]])).thenReturn(mockResponse)
    when(consignmentStatusRepositoryMock
      .updateConsignmentStatus(any[UUID], transferAgreementStatusTypeCaptor.capture(), transferAgreementStatusValueCaptor.capture(), any[Timestamp]))
      .thenReturn(Future.successful(1))

    val service = new TransferAgreementService(consignmentMetadataRepositoryMock, consignmentStatusRepositoryMock, fixedUuidSource, fixedTimeSource)
    val transferAgreementResult: TransferAgreementCompliance = service.addTransferAgreementCompliance(
      AddTransferAgreementComplianceInput(
        consignmentId,
        initialOpenRecords = true,
        appraisalSelectionSignedOff = true,
        sensitivityReviewSignedOff = true),
      userId
    ).futureValue

    val transferAgreementStatusType = transferAgreementStatusTypeCaptor.getValue
    val transferAgreementStatusValue = transferAgreementStatusValueCaptor.getValue

    transferAgreementStatusType shouldBe statusType
    transferAgreementStatusValue shouldBe statusValue
    transferAgreementResult.consignmentId shouldBe consignmentId
    transferAgreementResult.initialOpenRecords shouldBe true
    transferAgreementResult.appraisalSelectionSignedOff shouldBe true
    transferAgreementResult.sensitivityReviewSignedOff shouldBe true
  }

  "addTransferAgreementStatus" should "add the correct Transfer Agreement Status" in {
    val consignmentMetadataRepositoryMock = mock[ConsignmentMetadataRepository]
    val consignmentStatusRepositoryMock = mock[ConsignmentStatusRepository]
    val consignmentId = UUID.randomUUID()
    val consignmentStatusId = UUID.fromString("d2f2c8d8-2e1d-4996-8ad2-b26ed547d1aa")
    val statusType = "TransferAgreement"
    val statusValue = "InProgress"
    val createdTimestamp = Timestamp.from(now)

    val mockResponse = Future.successful(ConsignmentstatusRow(consignmentStatusId, consignmentId, statusType, statusValue, createdTimestamp))
    when(consignmentStatusRepositoryMock.addConsignmentStatus(any[ConsignmentstatusRow])).thenReturn(mockResponse)

    val service = new TransferAgreementService(consignmentMetadataRepositoryMock, consignmentStatusRepositoryMock, fixedUuidSource, fixedTimeSource)
    val result: ConsignmentstatusRow = service.addTransferAgreementStatus(consignmentId).futureValue

    result.consignmentstatusid shouldBe consignmentStatusId
    result.consignmentid shouldBe consignmentId
    result.statustype shouldBe statusType
    result.value shouldBe statusValue
    result.createddatetime shouldBe createdTimestamp
  }

  "updateTransferAgreementStatus" should "add the correct Transfer Agreement Status" in {
    val consignmentMetadataRepositoryMock = mock[ConsignmentMetadataRepository]
    val consignmentStatusRepositoryMock = mock[ConsignmentStatusRepository]
    val consignmentId = UUID.randomUUID()
    val statusType = "TransferAgreement"
    val statusValue = "Complete"

    val transferAgreementStatusTypeCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val transferAgreementStatusValueCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

    val mockResponse = Future.successful(1)
    when(consignmentStatusRepositoryMock
      .updateConsignmentStatus(any[UUID], transferAgreementStatusTypeCaptor.capture(), transferAgreementStatusValueCaptor.capture(), any[Timestamp]))
      .thenReturn(mockResponse)

    val service = new TransferAgreementService(consignmentMetadataRepositoryMock, consignmentStatusRepositoryMock, fixedUuidSource, fixedTimeSource)
    val result: Future[Int] = service.updateExistingTransferAgreementStatus(consignmentId, statusValue)

    val transferAgreementStatusType = transferAgreementStatusTypeCaptor.getValue
    val transferAgreementStatusValue = transferAgreementStatusValueCaptor.getValue

    transferAgreementStatusType shouldBe statusType
    transferAgreementStatusValue shouldBe statusValue
    result shouldEqual mockResponse
  }
}

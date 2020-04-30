package uk.gov.nationalarchives.tdr.api.service

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.AvmetadataRow
import uk.gov.nationalarchives.tdr.api.db.repository.AVMetadataRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.AVMetadataFields.AddAVMetadataInput
import uk.gov.nationalarchives.tdr.api.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class AVMetadataServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "addAVMetadata" should "create anti-virus metadata given the correct arguments" in {
    val fixedFileUuid = UUID.fromString("07a3a4bd-0281-4a6d-a4c1-8fa3239e1313")
    val dummyInstant = Instant.now()
    val dummyTimestamp = Timestamp.from(dummyInstant)
    val repositoryMock = mock[AVMetadataRepository]
    val mockResponse = Future.successful(Seq(AvmetadataRow(
      Some(fixedFileUuid),
      Some("software"),
      Some("value"),
      Some("software version"),
      Some("database version"),
      Some("result"),
      dummyTimestamp
    )))

    when(repositoryMock.addAVMetadata(any[Seq[AvmetadataRow]])).thenReturn(mockResponse)

    val service: AVMetadataService = new AVMetadataService(repositoryMock)
    val result = service.addAVMetadata(Seq(AddAVMetadataInput(
      fixedFileUuid,
      Some("software"),
      Some("value"),
      Some("software version"),
      Some("database version"),
      Some("result"),
      dummyInstant.toEpochMilli
    ))).await()

    result.length shouldBe 1
    val r = result.head
    r.fileId shouldBe fixedFileUuid
    r.software.get shouldBe "software"
    r.value.get shouldBe "value"
    r.softwareVersion.get shouldBe "software version"
    r.databaseVersion.get shouldBe "database version"
    r.result.get shouldBe "result"
    r.datetime shouldBe dummyInstant.toEpochMilli
  }
}

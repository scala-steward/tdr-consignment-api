package uk.gov.nationalarchives.tdr.api.db.repository

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import uk.gov.nationalarchives.Tables
import uk.gov.nationalarchives.Tables.{FilemetadataRow, FilestatusRow}
import uk.gov.nationalarchives.tdr.api.graphql.fields.FileMetadataFields.SHA256ServerSideChecksum
import uk.gov.nationalarchives.tdr.api.utils.TestContainerUtils._
import uk.gov.nationalarchives.tdr.api.utils.TestAuthUtils.userId
import uk.gov.nationalarchives.tdr.api.utils.{TestContainerUtils, TestUtils}

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class FileMetadataRepositorySpec extends TestContainerUtils with ScalaFutures with Matchers {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds))

  override def afterContainersStart(containers: containerDef.Container): Unit = {
    super.afterContainersStart(containers)
  }

  private def getFileStatusValue(fileId: UUID, utils: TestUtils): String =
    utils.getFileStatusResult(fileId, "Status Type").head

  private def checkFileMetadataExists(fileId: UUID, utils: TestUtils): Assertion = {
    utils.countFileMetadata(fileId) should be(1)
  }

  "countProcessedChecksumInConsignment" should "return 0 if a consignment has no files" in withContainers {
    case container: PostgreSQLContainer =>
      val db = container.database
      val utils = TestUtils(db)
      val fileMetadataRepository = new FileMetadataRepository(db)
      val consignmentId = UUID.fromString("c03fd4be-58c1-4cee-8d3c-d162bb4f7c01")

      utils.createConsignment(consignmentId, userId)

      val consignmentFiles = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentId).futureValue

      consignmentFiles shouldBe 0
  }

  "countProcessedChecksumInConsignment" should "return 0 if consignment has no checksum metadata for files" in withContainers {
    case container: PostgreSQLContainer =>
      val db = container.database
      val utils = TestUtils(db)
      val fileMetadataRepository = new FileMetadataRepository(db)
      val consignmentId = UUID.fromString("64456c78-49bb-4bff-85c8-2fff053b9f8d")
      val fileOneId = UUID.fromString("2c2dedf5-56a1-497f-9f8a-19102739a056")
      val fileTwoId = UUID.fromString("c2d5c569-0fc4-4688-9523-157c4028f1b1")
      val fileThreeId = UUID.fromString("317c7084-d3d4-435b-acd2-cfb317793844")

      utils.createConsignment(consignmentId, userId)
      utils.createFile(fileOneId, consignmentId)
      utils.createFile(fileTwoId, consignmentId)
      utils.createFile(fileThreeId, consignmentId)
      //  We have created files, but not added any fileMetadata for those files to the FileMetadata repository.
      //  Thus, calling the countProcessedChecksumInConsignment method should return 0.

      val consignmentFiles = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentId).futureValue

      consignmentFiles shouldBe 0
  }

  "countProcessedChecksumInConsignment" should "return the number of fileMetadata for files in a specified consignment" in withContainers {
    case container: PostgreSQLContainer =>
      val db = container.database
      val utils = TestUtils(db)
      val fileMetadataRepository = new FileMetadataRepository(db)
      val consignmentOne = UUID.fromString("049a11d7-06f5-4b11-b786-640000de76e2")
      val consignmentTwo = UUID.fromString("dc2acd2c-3370-44fe-aeae-16aee30dc410")
      val fileOneId = "5a102380-25ec-4881-a484-87d56fe9a0b4"
      val fileTwoId = "f616272d-4d80-44cf-90de-996c3984847d"
      val fileThreeId = "40c67a87-70c0-4424-b934-a395459ddbe1"
      val metadataOneId = "e4440f43-20c6-4b6c-811d-349e633617e4"
      val metadataTwoId = "7e5abff1-1d86-4d7d-8ab4-c6a2934ec611"
      val metadataThreeId = "efbd5da6-909a-45b1-964f-87a3baf48816"

      //  Need to create a consignment with files in it
      utils.createConsignment(consignmentOne, userId)
      utils.createConsignment(consignmentTwo, userId)
      utils.createFile(UUID.fromString(fileOneId), consignmentOne)
      utils.createFile(UUID.fromString(fileTwoId), consignmentOne)
      utils.createFile(UUID.fromString(fileThreeId), consignmentTwo)

      //  Then need to add data to the FileMetadata repository for these files
      utils.addFileMetadata(metadataOneId, fileOneId, SHA256ServerSideChecksum)
      utils.addFileMetadata(metadataTwoId, fileTwoId, SHA256ServerSideChecksum)
      utils.addFileMetadata(metadataThreeId, fileThreeId, SHA256ServerSideChecksum)

      val fileMetadataFilesConsignmentOne = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentOne).futureValue
      val fileMetadataFilesConsignmentTwo = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentTwo).futureValue

      fileMetadataFilesConsignmentOne shouldBe 2
      fileMetadataFilesConsignmentTwo shouldBe 1
  }

  "countProcessedChecksumInConsignment" should "return number of fileMetadata rows with repetitive data filtered out" in withContainers {
    case container: PostgreSQLContainer =>
      val db = container.database
      val utils = TestUtils(db)
      val fileMetadataRepository = new FileMetadataRepository(db)
      val consignmentId = UUID.fromString("c6f78fef-704a-46a8-82c0-afa465199e66")
      val fileOneId = "20e0676a-f0a1-4051-9540-e7df1344ac11"
      val fileTwoId = "b5111f11-4dca-4f92-8239-505da567b9d0"
      val metadataId = "f4440f43-20c6-4b6c-811d-349e633617e5"

      utils.createConsignment(consignmentId, userId)
      utils.createFile(UUID.fromString(fileOneId), consignmentId)
      utils.createFile(UUID.fromString(fileTwoId), consignmentId)

      (1 to 7).foreach { _ => utils.addFileMetadata(UUID.randomUUID().toString, fileOneId, SHA256ServerSideChecksum) }

      utils.addFileMetadata(metadataId, fileTwoId, SHA256ServerSideChecksum)
      val fileMetadataFiles = fileMetadataRepository.countProcessedChecksumInConsignment(consignmentId).futureValue

      fileMetadataFiles shouldBe 2
  }

  "addFileMetadata" should "add metadata with the correct values" in withContainers {
    case container: PostgreSQLContainer =>
      val db = container.database
      val utils = TestUtils(db)
      val fileMetadataRepository = new FileMetadataRepository(db)
      val consignmentId = UUID.fromString("306c526b-d099-470b-87c8-df7bd0aa225a")
      val fileId = UUID.fromString("ba176f90-f0fd-42ef-bb28-81ba3ffb6f05")
      utils.addFileProperty("FileProperty")
      utils.createConsignment(consignmentId, userId)
      utils.createFile(fileId, consignmentId)
      val input = Seq(FilemetadataRow(UUID.randomUUID(), fileId, "value", Timestamp.from(Instant.now()), UUID.randomUUID(), "FileProperty"))
      val result = fileMetadataRepository.addFileMetadata(input).futureValue.head
      result.propertyname should equal("FileProperty")
      result.value should equal("value")
      checkFileMetadataExists(fileId, utils)
  }

  "addChecksumMetadata" should "update the checksum validation field on the file table" in withContainers {
    case container: PostgreSQLContainer =>
      val db = container.database
      val utils = TestUtils(db)
      val fileMetadataRepository = new FileMetadataRepository(db)
      val consignmentId = UUID.fromString("f25fc436-12f1-48e8-8e1a-3fada106940a")
      val fileId = UUID.fromString("59ce7106-57f2-48ff-b451-4148e6bf74f9")
      utils.createConsignment(consignmentId, userId)
      utils.createFile(fileId, consignmentId)
      utils.addFileProperty("FileProperty")
      utils.addFileMetadata(UUID.randomUUID().toString, fileId.toString, "FileProperty")
      val input = FilemetadataRow(UUID.randomUUID(), fileId, "value", Timestamp.from(Instant.now()), UUID.randomUUID(), "FileProperty")
      val statusInput = FilestatusRow(UUID.randomUUID(), fileId, "Status Type", "Value", Timestamp.from(Instant.now()))
      fileMetadataRepository.addChecksumMetadata(input, statusInput).futureValue
      getFileStatusValue(fileId, utils) should equal("Value")
  }

  "getSingleFileMetadata" should "return the correct metadata" in withContainers {
    case container: PostgreSQLContainer =>
      val db = container.database
      val utils = TestUtils(db)
      val fileMetadataRepository = new FileMetadataRepository(db)
      val consignmentId = UUID.fromString("4c935c42-502c-4b89-abce-2272584655e1")
      val fileId = UUID.fromString("4d5a5a00-77b4-4a97-aa3f-a75f7b13f284")
      utils.createConsignment(consignmentId, userId)
      utils.createFile(fileId, consignmentId)
      utils.addFileProperty("FileProperty")
      utils.addFileMetadata(UUID.randomUUID().toString, fileId.toString, "FileProperty")
      val response = fileMetadataRepository.getSingleFileMetadata(fileId, "FileProperty").futureValue.head
      response.value should equal("Result of FileMetadata processing")
      response.propertyname should equal("FileProperty")
      response.fileid should equal(fileId)
  }

  "getFileMetadata" should "return the correct metadata for the consignment" in withContainers {
    case container: PostgreSQLContainer =>
      val db = container.database
      val utils = TestUtils(db)
      val fileMetadataRepository = new FileMetadataRepository(db)
      val consignmentId = UUID.fromString("4c935c42-502c-4b89-abce-2272584655e1")
      val fileIdOne = UUID.fromString("4d5a5a00-77b4-4a97-aa3f-a75f7b13f284")
      val fileIdTwo = UUID.fromString("664f07a5-ab1d-4d66-abea-d97d81cd7bec")
      utils.createConsignment(consignmentId, userId)
      utils.createFile(fileIdOne, consignmentId)
      utils.createFile(fileIdTwo, consignmentId)
      utils.addFileProperty("FilePropertyOne")
      utils.addFileProperty("FilePropertyTwo")
      utils.addFileMetadata(UUID.randomUUID().toString, fileIdOne.toString, "FilePropertyOne")
      utils.addFileMetadata(UUID.randomUUID().toString, fileIdOne.toString, "FilePropertyTwo")
      utils.addFileMetadata(UUID.randomUUID().toString, fileIdTwo.toString, "FilePropertyOne")

      val response = fileMetadataRepository.getFileMetadata(consignmentId).futureValue

      response.length should equal(3)
      val filesMap: Map[UUID, Seq[Tables.FilemetadataRow]] = response.groupBy(_.fileid)
      val fileOneMetadata = filesMap.get(fileIdOne)
      val fileTwoMetadata = filesMap.get(fileIdTwo)
      val fileOneProperties = fileOneMetadata.get.groupBy(_.propertyname)
      fileOneProperties("FilePropertyOne").head.value should equal("Result of FileMetadata processing")
      fileOneProperties("FilePropertyTwo").head.value should equal("Result of FileMetadata processing")
      fileTwoMetadata.get.head.propertyname should equal("FilePropertyOne")
      fileTwoMetadata.get.head.value should equal("Result of FileMetadata processing")
  }

  "getFileMetadata" should "return the correct metadata for the given consignment and selected file ids" in withContainers {
    case container: PostgreSQLContainer =>
      val db = container.database
      val utils = TestUtils(db)
      val fileMetadataRepository = new FileMetadataRepository(db)
      val consignmentId = UUID.fromString("4c935c42-502c-4b89-abce-2272584655e1")
      val fileIdOne = UUID.fromString("4d5a5a00-77b4-4a97-aa3f-a75f7b13f284")
      val fileIdTwo = UUID.fromString("664f07a5-ab1d-4d66-abea-d97d81cd7bec")
      utils.createConsignment(consignmentId, userId)
      utils.createFile(fileIdOne, consignmentId)
      utils.createFile(fileIdTwo, consignmentId)
      utils.addFileProperty("FilePropertyOne")
      utils.addFileProperty("FilePropertyTwo")
      utils.addFileMetadata(UUID.randomUUID().toString, fileIdOne.toString, "FilePropertyOne")
      utils.addFileMetadata(UUID.randomUUID().toString, fileIdOne.toString, "FilePropertyTwo")
      utils.addFileMetadata(UUID.randomUUID().toString, fileIdTwo.toString, "FilePropertyOne")

      val selectedFileIds: Set[UUID] = Set(fileIdTwo)
      val response = fileMetadataRepository.getFileMetadata(consignmentId, Some(selectedFileIds)).futureValue

      response.length should equal(1)
      val filesMap: Map[UUID, Seq[Tables.FilemetadataRow]] = response.groupBy(_.fileid)
      val fileTwoMetadata = filesMap.get(fileIdTwo)
      fileTwoMetadata.get.head.propertyname should equal("FilePropertyOne")
      fileTwoMetadata.get.head.value should equal("Result of FileMetadata processing")
  }
}

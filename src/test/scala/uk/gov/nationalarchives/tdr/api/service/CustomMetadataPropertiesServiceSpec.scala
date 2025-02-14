package uk.gov.nationalarchives.tdr.api.service

import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.Tables.{FilepropertyRow, FilepropertydependenciesRow, FilepropertyvaluesRow}
import uk.gov.nationalarchives.tdr.api.db.repository.CustomMetadataPropertiesRepository

import java.sql.Timestamp
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class CustomMetadataPropertiesServiceSpec extends AnyFlatSpec with MockitoSugar with Matchers with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  "getClosureMetadata" should "correctly return sequence of metadataField" in {
    val customMetadataPropertiesRepository = mock[CustomMetadataPropertiesRepository]
    val mockPropertyResponse = Future(Seq(
      FilepropertyRow("closureType", None, Some("Closure Type"), Timestamp.from(Instant.now()),
        None, Some("Defined"), Some("text"), Some(true), None, Some("Closure"))
    ))
    val mockPropertyValuesResponse = Future(Seq(
      FilepropertyvaluesRow("closureType", "closed_for", None, None, None)
    ))
    val mockPropertyDependenciesResponse = Future(Seq(
      FilepropertydependenciesRow(3, "ClosurePeriod", None)
    ))

    when(customMetadataPropertiesRepository.getClosureMetadataProperty).thenReturn(mockPropertyResponse)
    when(customMetadataPropertiesRepository.getClosureMetadataValues).thenReturn(mockPropertyValuesResponse)
    when(customMetadataPropertiesRepository.getClosureMetadataDependencies).thenReturn(mockPropertyDependenciesResponse)

    val service = new CustomMetadataPropertiesService(customMetadataPropertiesRepository)
    val response = service.getClosureMetadata.futureValue

    response.size should equal(1)
    response.head.values.head.value should equal("closed_for")
    response.head.values.head.dependencies.isEmpty should equal(true)
  }

  "getClosureMetadata" should "correctly return sequence of metadataField with dependencies" in {
    val customMetadataPropertiesRepository = mock[CustomMetadataPropertiesRepository]
    val mockPropertyResponse = Future(Seq(
      FilepropertyRow("closureType", None, Some("Closure Type"), Timestamp.from(Instant.now()),
        None, Some("Defined"), Some("text"), Some(true), None, Some("Closure")),
      FilepropertyRow("ClosurePeriod", None, Some("Closure Type"), Timestamp.from(Instant.now()),
        None, Some("Defined"), Some("text"), Some(true), None, Some("Closure"))
    ))
    val mockPropertyValuesResponse = Future(Seq(
      FilepropertyvaluesRow("closureType", "closed_for", None, Some(3), None)
    ))
    val mockPropertyDependenciesResponse = Future(Seq(
      FilepropertydependenciesRow(3, "ClosurePeriod", None)
    ))

    when(customMetadataPropertiesRepository.getClosureMetadataProperty).thenReturn(mockPropertyResponse)
    when(customMetadataPropertiesRepository.getClosureMetadataValues).thenReturn(mockPropertyValuesResponse)
    when(customMetadataPropertiesRepository.getClosureMetadataDependencies).thenReturn(mockPropertyDependenciesResponse)

    val service = new CustomMetadataPropertiesService(customMetadataPropertiesRepository)
    val response = service.getClosureMetadata.futureValue

    response.size should equal(2)
    response.head.values.size should equal(1)
    response(1).values.size should equal(0)
  }
}

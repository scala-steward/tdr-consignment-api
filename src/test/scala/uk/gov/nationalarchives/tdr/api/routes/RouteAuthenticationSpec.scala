package uk.gov.nationalarchives.tdr.api.routes

import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import uk.gov.nationalarchives.tdr.api.http.Routes
import uk.gov.nationalarchives.tdr.api.utils.TestContainerUtils._
import uk.gov.nationalarchives.tdr.api.utils.TestAuthUtils._
import uk.gov.nationalarchives.tdr.api.utils.{TestContainerUtils, TestUtils}

class RouteAuthenticationSpec extends TestContainerUtils with Matchers with ScalatestRouteTest {

  override def afterContainersStart(containers: containerDef.Container): Unit = super.afterContainersStart(containers)

  "The api" should "return ok" in withContainers {
    case container: PostgreSQLContainer =>
      val route: Route = new Routes(ConfigFactory.load(), container.session).route
      Get("/healthcheck") ~> route ~> check {
        responseAs[String] shouldEqual "OK"
      }
  }

  "The api" should "return a rejected credentials error" in withContainers {
    case container: PostgreSQLContainer =>
      val route: Route = new Routes(ConfigFactory.load(), container.session).route
      Post("/graphql") ~> addCredentials(invalidToken) ~> route ~> check {
        rejection shouldBe AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", Some("tdr")))
      }
  }

  "The api" should "return a missing credentials error" in withContainers {
    case container: PostgreSQLContainer =>
      val route: Route = new Routes(ConfigFactory.load(), container.session).route
      Post("/graphql") ~> route ~> check {
        rejection shouldBe AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", Some("tdr")))
      }
  }

  "The api" should "return a valid response with a valid token" in withContainers {
    case container: PostgreSQLContainer =>
      val route: Route = new Routes(ConfigFactory.load(), container.session).route
      val query: String = """{"query":"{getSeries(body:\"Body\"){seriesid}}"}"""
      Post("/graphql").withEntity(ContentTypes.`application/json`, query) ~> addCredentials(validUserToken()) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
  }

  "The db" should "return ok when there is at least one transferring body in the db" in withContainers {
    case container: PostgreSQLContainer =>
      val route: Route = new Routes(ConfigFactory.load(), container.session).route
      Get("/healthcheck-full") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
  }

  "The db" should "return 500 Internal Server Error if there are no transferring bodies in the db" in withContainers {
    case container: PostgreSQLContainer =>
      TestUtils(container.database).deleteSeriesAndBody()
      val route: Route = new Routes(ConfigFactory.load(), container.session).route
      Get("/healthcheck-full") ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
      }
  }
}

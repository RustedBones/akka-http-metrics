package fr.davit.akka.http.metrics.core.scaladsl.server

import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.IntNumber
import akka.http.scaladsl.testkit.ScalatestRouteTest
import fr.davit.akka.http.metrics.core.TestRegistry
import org.scalatest.{FlatSpec, Matchers}

class HttpMetricsDirectivesSpec extends FlatSpec with Matchers with ScalatestRouteTest {

  import HttpMetricsDirectives._


  "HttpMetricsDirectives" should "expose the registry" in {
    implicit val marshaller = StringMarshaller.compose[TestRegistry](r => s"active: ${r.active.value()}")
    val registry = new TestRegistry()
    registry.active.inc()

    val route = path("metrics") {
      metrics(registry)
    }

    Get("/metrics") ~> route ~> check {
      responseAs[String] shouldBe "active: 1"
    }
  }

  it should "label segment in headers" in {
    val route = pathLabeled("user" / LongNumber, "user/:userId") { _ =>
      complete(StatusCodes.OK)
    }

    Get("/user/1234") ~> route ~> check {
      headers should contain(SegmentLabelHeader(0, 4, "/user/:userId"))
    }
  }

  it should "accumulate segments" in {
    val route = pathPrefixLabeled("user" / LongNumber, "user/:userId") { _ =>
      pathLabeled("address" / IntNumber, "address/:addressId") { _ =>
        complete(StatusCodes.OK)
      }
    }

    Get("/user/1234/address/1") ~> route ~> check {
      headers should contain allOf(
        SegmentLabelHeader(0, 4, "/user/:userId"),
        SegmentLabelHeader(4, 8, "/address/:addressId")
      )
    }
  }


}

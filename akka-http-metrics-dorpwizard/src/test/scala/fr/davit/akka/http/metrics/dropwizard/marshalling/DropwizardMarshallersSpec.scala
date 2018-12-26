package fr.davit.akka.http.metrics.dropwizard.marshalling

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.testkit.ScalatestRouteTest
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._
import fr.davit.akka.http.metrics.dropwizard.DropwizardRegistry
import org.scalatest.{FlatSpec, Matchers}
import spray.json.{DefaultJsonProtocol, JsValue}

class DropwizardMarshallersSpec extends FlatSpec with Matchers with ScalatestRouteTest {

  final case class JsonResponse(metrics: Map[String, JsValue])

  trait Fixture extends SprayJsonSupport with DefaultJsonProtocol with DropwizardMarshallers {
    implicit val metricsFormat = jsonFormat1(JsonResponse)

    val registry = DropwizardRegistry()
    registry.underlying.counter("other.metric")
  }

  "DropwizardMarshallers" should "expose metrics as json format" in new Fixture {
    Get() ~> metrics(registry) ~> check {
      val json = responseAs[JsonResponse]
      println(json)
      json.metrics.keys should contain theSameElementsAs Seq(
        "akka.http.requests",
        "akka.http.requests.active",
        "akka.http.requests.errors",
        "akka.http.requests.duration",
        "akka.http.requests.size",
        "akka.http.responses.size",
        "other.metric"
      )
    }
  }

}

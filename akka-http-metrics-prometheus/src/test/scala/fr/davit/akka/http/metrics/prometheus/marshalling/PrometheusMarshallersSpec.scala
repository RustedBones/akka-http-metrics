package fr.davit.akka.http.metrics.prometheus.marshalling

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.StatusGroupDimension
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives.metrics
import fr.davit.akka.http.metrics.prometheus.PrometheusRegistry
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration._

class PrometheusMarshallersSpec extends FlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll {

  trait Fixture extends PrometheusMarshallers {
    val registry = PrometheusRegistry()

    io.prometheus.client.Counter
      .build("other_metric", "An other metric")
      .register(registry.underlying)
  }

  override def afterAll(): Unit = {
    cleanUp()
    super.afterAll()
  }

  "PrometheusMarshallers" should "expose metrics as prometheus format" in new Fixture {
    // register labeled metrics so they appear at least once
    // use metrics so they appear in the report
    val dimensions = Seq(StatusGroupDimension(StatusCodes.OK))
    registry.requests.inc()
    registry.receivedBytes.update(10)
    registry.active.inc()
    registry.responses.inc(dimensions)
    registry.errors.inc()
    registry.duration.observe(1.second, dimensions)
    registry.sentBytes.update(10)

    Get() ~> metrics(registry) ~> check {
      response.entity.contentType shouldBe PrometheusMarshallers.PrometheusContentType
      val text = responseAs[String]
      // println(text)
      val metrics = text
        .split('\n')
        .filterNot(_.startsWith("#"))
        .map(_.takeWhile(c => c != ' ' && c != '{'))
        .distinct
      metrics should contain theSameElementsAs Seq(
        "akka_http_requests_active",
        "akka_http_requests_total",
        "akka_http_requests_size_bytes",
        "akka_http_requests_size_bytes_count",
        "akka_http_requests_size_bytes_sum",
        "akka_http_responses_total",
        "akka_http_responses_errors_total",
        "akka_http_responses_duration_seconds",
        "akka_http_responses_duration_seconds_count",
        "akka_http_responses_duration_seconds_sum",
        "akka_http_responses_size_bytes",
        "akka_http_responses_size_bytes_count",
        "akka_http_responses_size_bytes_sum",
        "other_metric"
      )
    }
  }
}
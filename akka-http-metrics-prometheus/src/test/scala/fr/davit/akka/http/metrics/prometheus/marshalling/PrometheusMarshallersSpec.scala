package fr.davit.akka.http.metrics.prometheus.marshalling

import akka.http.scaladsl.testkit.ScalatestRouteTest
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives.metrics
import fr.davit.akka.http.metrics.prometheus.PrometheusRegistry
import org.scalatest.{FlatSpec, Matchers}

class PrometheusMarshallersSpec extends FlatSpec with Matchers with ScalatestRouteTest {

  trait Fixture extends PrometheusMarshallers {
    val registry = PrometheusRegistry()
    io.prometheus.client.Counter
      .build("other_metric", "An other metric")
      .register(registry.underlying)
  }

  "PrometheusMarshallers" should "expose metrics as prometheus format" in new Fixture {
    Get() ~> metrics(registry) ~> check {
      response.entity.contentType shouldBe PrometheusMarshallers.PrometheusContentType
      val text = responseAs[String]
      println(text)
      val metrics = text
        .split('\n')
        .filterNot(_.startsWith("#"))
        .map(_.takeWhile(c => c != ' ' && c != '{'))
        .distinct
      metrics should contain theSameElementsAs Seq(
        "akka_http_requests_total",
        "akka_http_requests_errors_total",
        "akka_http_requests_active",
        "akka_http_requests_duration_seconds",
        "akka_http_requests_duration_seconds_count",
        "akka_http_requests_duration_seconds_sum",
        "akka_http_requests_size_bytes",
        "akka_http_requests_size_bytes_count",
        "akka_http_requests_size_bytes_sum",
        "akka_http_responses_size_bytes",
        "akka_http_responses_size_bytes_count",
        "akka_http_responses_size_bytes_sum",
        "other_metric"
      )
    }
  }
}
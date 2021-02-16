/*
 * Copyright 2019 Michel Davit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.davit.akka.http.metrics.prometheus.marshalling

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.StatusGroupDimension
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives.metrics
import fr.davit.akka.http.metrics.prometheus.{PrometheusRegistry, PrometheusSettings}
import io.prometheus.client.CollectorRegistry
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class PrometheusMarshallersSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll {

  trait Fixture extends PrometheusMarshallers {

    val registry = PrometheusRegistry(
      new CollectorRegistry(),
      PrometheusSettings.default.withIncludeStatusDimension(true)
    )

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
    registry.requestsActive.inc()
    registry.requestsSize.update(10)
    registry.responses.inc(dimensions)
    registry.responsesErrors.inc(dimensions)
    registry.responsesDuration.observe(1.second, dimensions)
    registry.responsesSize.update(10, dimensions)
    registry.connections.inc()
    registry.connectionsActive.inc()

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
        "akka_http_requests_total",
        "akka_http_requests_active",
        "akka_http_requests_created",
        "akka_http_requests_size_bytes_bucket",
        "akka_http_requests_size_bytes_count",
        "akka_http_requests_size_bytes_sum",
        "akka_http_requests_size_bytes_created",
        "akka_http_responses_total",
        "akka_http_responses_created",
        "akka_http_responses_errors_total",
        "akka_http_responses_errors_created",
        "akka_http_responses_duration_seconds_bucket",
        "akka_http_responses_duration_seconds_count",
        "akka_http_responses_duration_seconds_sum",
        "akka_http_responses_duration_seconds_created",
        "akka_http_responses_size_bytes_bucket",
        "akka_http_responses_size_bytes_count",
        "akka_http_responses_size_bytes_sum",
        "akka_http_responses_size_bytes_created",
        "akka_http_connections_total",
        "akka_http_connections_active",
        "akka_http_connections_created",
        "other_metric_total",
        "other_metric_created"
      )
    }
  }
}

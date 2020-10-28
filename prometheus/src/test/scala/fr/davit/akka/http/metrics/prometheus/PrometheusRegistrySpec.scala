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

package fr.davit.akka.http.metrics.prometheus

import akka.http.scaladsl.model.{HttpMethods, StatusCodes}
import fr.davit.akka.http.metrics.core.Dimension
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{MethodDimension, PathDimension, StatusGroupDimension}
import io.prometheus.client.CollectorRegistry
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable
import scala.concurrent.duration._

case class CustomDimension(key: String, value: String) extends Dimension {}

class PrometheusRegistrySpec extends AnyFlatSpec with Matchers {

  val customDimensions = immutable.Seq(
    CustomDimension("test_key", "test_value")
  )

  val dimensions = Seq(
    MethodDimension(HttpMethods.GET),
    PathDimension("/api"),
    StatusGroupDimension(StatusCodes.OK)
  ) ++ customDimensions

  trait Fixture {
    val registry = PrometheusRegistry(new CollectorRegistry())

    def underlyingCounterValue(name: String, dims: Seq[Dimension] = Seq.empty): Long = {
      registry.underlying.getSampleValue(name, dims.map(_.key).toArray, dims.map(_.value).toArray).toLong
    }

    def underlyingHistogramValue(name: String, dims: Seq[Dimension] = Seq.empty): Double = {
      registry.underlying.getSampleValue(s"${name}_sum", dims.map(_.key).toArray, dims.map(_.value).toArray)
    }
  }

  trait DimensionFixture extends Fixture {
    override val registry = PrometheusRegistry(
      new CollectorRegistry(),
      PrometheusSettings.default
        .withIncludeMethodDimension(true)
        .withIncludePathDimension(true)
        .withIncludeStatusDimension(true)
    )
  }

  trait MetricsNamesFixture extends Fixture {
    override val registry = PrometheusRegistry(
      new CollectorRegistry(),
      PrometheusSettings.default
        .withNamespace("test_server")
        .withMetricsNames(
          PrometheusMetricsNames.default
            .withConnectionsActive("test_connections_active")
            .withRequestsActive("test_requests_active")
            .withConnections("test_connections_total")
            .withResponsesDuration("test_responses_duration_seconds")
            .withResponsesErrors("test_responses_errors_total")
            .withRequests("test_requests_total")
            .withRequestSize("test_requests_size_bytes")
            .withResponses("test_responses_total")
            .withResponseSize("test_responses_size_bytes")
        )
    )
  }

  it should "set requestsActive metrics in the underlying registry" in new Fixture {
    registry.requestsActive.inc()
    underlyingCounterValue("akka_http_requests_active") shouldBe 1L
  }

  it should "set requestsActive metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.requestsActive.inc()
    underlyingCounterValue("test_server_test_requests_active") shouldBe 1L
  }

  it should "set requests metrics in the underlying registry" in new Fixture {
    registry.requests.inc()
    underlyingCounterValue("akka_http_requests_total") shouldBe 1L
  }

  it should "set requests metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.requests.inc()
    underlyingCounterValue("test_server_test_requests_total") shouldBe 1L
  }

  it should "set requestsSize metrics in the underlying registry" in new Fixture {
    registry.requestsSize.update(3)
    underlyingHistogramValue("akka_http_requests_size_bytes") shouldBe 3L
  }

  it should "set requestsSize metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.requestsSize.update(3)
    underlyingHistogramValue("test_server_test_requests_size_bytes") shouldBe 3L
  }

  it should "set responses metrics in the underlying registry" in new Fixture {
    registry.responses.inc()
    underlyingCounterValue("akka_http_responses_total") shouldBe 1L
  }

  it should "set responses metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.responses.inc()
    underlyingCounterValue("test_server_test_responses_total") shouldBe 1L
  }

  it should "set responses metrics in the underlying registry with dimensions" in new DimensionFixture {
    registry.responses.inc(dimensions)
    underlyingCounterValue("akka_http_responses_total", dimensions) shouldBe 1L
  }

  it should "set responsesErrors metrics in the underlying registry" in new Fixture {
    registry.responsesErrors.inc()
    underlyingCounterValue("akka_http_responses_errors_total") shouldBe 1L
  }

  it should "set responsesErrors metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.responsesErrors.inc()
    underlyingCounterValue("test_server_test_responses_errors_total") shouldBe 1L
  }

  it should "set responsesErrors metrics in the underlying registry with dimensions" in new DimensionFixture {
    registry.responsesErrors.inc(dimensions)
    underlyingCounterValue("akka_http_responses_errors_total", dimensions) shouldBe 1L
  }

  it should "set responsesDuration metrics in the underlying registry" in new Fixture {
    registry.responsesDuration.observe(3.seconds)
    underlyingHistogramValue("akka_http_responses_duration_seconds") shouldBe 3.0
  }

  it should "set responsesDuration metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.responsesDuration.observe(3.seconds)
    underlyingHistogramValue("test_server_test_responses_duration_seconds") shouldBe 3.0
  }

  it should "set responsesDuration metrics in the underlying registry with dimension" in new DimensionFixture {
    registry.responsesDuration.observe(3.seconds, dimensions)
    underlyingHistogramValue("akka_http_responses_duration_seconds", dimensions) shouldBe 3.0
  }

  it should "set responsesSize metrics in the underlying registry" in new Fixture {
    registry.responsesSize.update(3)
    underlyingHistogramValue("akka_http_responses_size_bytes") shouldBe 3L
  }

  it should "set responsesSize metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.responsesSize.update(3)
    underlyingHistogramValue("test_server_test_responses_size_bytes") shouldBe 3L
  }

  it should "set responsesSize metrics in the underlying registry with dimensions" in new DimensionFixture {
    registry.responsesSize.update(3, dimensions)
    underlyingHistogramValue("akka_http_responses_size_bytes", dimensions) shouldBe 3L
  }

  it should "set connectionsActive metrics in the underlying registry" in new Fixture {
    registry.connectionsActive.inc()
    underlyingCounterValue("akka_http_connections_active") shouldBe 1L
  }

  it should "set connectionsActive metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.connectionsActive.inc()
    underlyingCounterValue("test_server_test_connections_active") shouldBe 1L
  }

  it should "set connections metrics in the underlying registry" in new Fixture {
    registry.connections.inc()
    underlyingCounterValue("akka_http_connections_total") shouldBe 1L
  }

  it should "set connections metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.connections.inc()
    underlyingCounterValue("test_server_test_connections_total") shouldBe 1L
  }
}

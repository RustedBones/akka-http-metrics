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

package fr.davit.pekko.http.metrics.prometheus

import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import fr.davit.pekko.http.metrics.core._
import io.prometheus.client.CollectorRegistry
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class PrometheusRegistrySpec extends AnyFlatSpec with Matchers {

  object CustomRequestLabeler extends HttpRequestLabeler {
    override def name                                = "custom_request_dim"
    def label                                        = "custom_request_label"
    override def label(request: HttpRequest): String = label
  }

  object CustomResponseLabeler extends HttpResponseLabeler {
    override def name                                  = "custom_request_dim"
    def label                                          = "custom_response_label"
    override def label(response: HttpResponse): String = label
  }

  val serverDimensions = List(
    Dimension("server_dim", "server_label")
  )
  val requestsDimensions = List(
    Dimension(MethodLabeler.name, "GET"),
    Dimension(CustomRequestLabeler.name, CustomRequestLabeler.label)
  )
  val responsesDimensions = List(
    Dimension(StatusGroupLabeler.name, "2xx"),
    Dimension(PathLabeler.name, "/api"),
    Dimension(CustomResponseLabeler.name, CustomResponseLabeler.label)
  )

  trait Fixture {

    val registry = PrometheusRegistry(
      new CollectorRegistry(),
      PrometheusSettings.default
    )

    def underlyingCounterValue(name: String, dims: Seq[Dimension] = Seq.empty): Long = {
      registry.underlying.getSampleValue(name, dims.map(_.name).toArray, dims.map(_.label).toArray).toLong
    }

    def underlyingHistogramValue(name: String, dims: Seq[Dimension] = Seq.empty): Double = {
      registry.underlying.getSampleValue(s"${name}_sum", dims.map(_.name).toArray, dims.map(_.label).toArray)
    }
  }

  trait DimensionFixture extends Fixture {

    override val registry = PrometheusRegistry(
      new CollectorRegistry(),
      PrometheusSettings.default
        .withIncludeMethodDimension(true)
        .withIncludePathDimension(true)
        .withIncludeStatusDimension(true)
        .withServerDimensions(serverDimensions: _*)
        .withCustomDimensions(CustomRequestLabeler, CustomResponseLabeler)
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

  it should "not have any dimensions by default" in new Fixture {
    registry.serverDimensions shouldBe empty
    registry.requestsDimensions shouldBe empty
    registry.responsesDimensions shouldBe empty
  }

  it should "add proper dimensions when configured" in new DimensionFixture {
    registry.serverDimensions should contain theSameElementsInOrderAs serverDimensions.map(_.name)
    registry.requestsDimensions should contain theSameElementsInOrderAs requestsDimensions.map(_.name)
    registry.responsesDimensions should contain theSameElementsInOrderAs responsesDimensions.map(_.name)
  }

  it should "set requestsActive metrics in the underlying registry" in new Fixture {
    registry.requestsActive.inc()
    underlyingCounterValue("akka_http_requests_active") shouldBe 1L
  }

  it should "set requestsActive metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.requestsActive.inc()
    underlyingCounterValue("test_server_test_requests_active") shouldBe 1L
  }

  it should "set requestsActive metrics in the underlying registry with dimensions" in new DimensionFixture {
    val dim = serverDimensions ++ requestsDimensions
    registry.requestsActive.inc(dim)
    underlyingCounterValue("akka_http_requests_active", dim) shouldBe 1L
  }

  it should "set requests metrics in the underlying registry" in new Fixture {
    registry.requests.inc()
    underlyingCounterValue("akka_http_requests_total") shouldBe 1L
  }

  it should "set requests metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.requests.inc()
    underlyingCounterValue("test_server_test_requests_total") shouldBe 1L
  }

  it should "set requests metrics in the underlying registry with dimensions" in new DimensionFixture {
    val dims = serverDimensions ++ requestsDimensions
    registry.requests.inc(dims)
    underlyingCounterValue("akka_http_requests_total", dims) shouldBe 1L
  }

  it should "set requestsSize metrics in the underlying registry" in new Fixture {
    registry.requestsSize.update(3)
    underlyingHistogramValue("akka_http_requests_size_bytes") shouldBe 3L
  }

  it should "set requestsSize metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.requestsSize.update(3)
    underlyingHistogramValue("test_server_test_requests_size_bytes") shouldBe 3L
  }

  it should "set requestsSize metrics in the underlying registry with dimensions" in new DimensionFixture {
    val dims = serverDimensions ++ requestsDimensions
    registry.requestsSize.update(3, dims)
    underlyingHistogramValue("akka_http_requests_size_bytes", dims) shouldBe 3L
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
    val dims = serverDimensions ++ requestsDimensions ++ responsesDimensions
    registry.responses.inc(dims)
    underlyingCounterValue("akka_http_responses_total", dims) shouldBe 1L
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
    val dims = serverDimensions ++ requestsDimensions ++ responsesDimensions
    registry.responsesErrors.inc(dims)
    underlyingCounterValue("akka_http_responses_errors_total", dims) shouldBe 1L
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
    val dims = serverDimensions ++ requestsDimensions ++ responsesDimensions
    registry.responsesDuration.observe(3.seconds, dims)
    underlyingHistogramValue("akka_http_responses_duration_seconds", dims) shouldBe 3.0
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
    val dims = serverDimensions ++ requestsDimensions ++ responsesDimensions
    registry.responsesSize.update(3, dims)
    underlyingHistogramValue("akka_http_responses_size_bytes", dims) shouldBe 3L
  }

  it should "set connectionsActive metrics in the underlying registry" in new Fixture {
    registry.connectionsActive.inc()
    underlyingCounterValue("akka_http_connections_active") shouldBe 1L
  }

  it should "set connectionsActive metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.connectionsActive.inc()
    underlyingCounterValue("test_server_test_connections_active") shouldBe 1L
  }

  it should "set connectionsActive metrics in the underlying registry with dimensions" in new DimensionFixture {
    val dims = serverDimensions
    registry.connectionsActive.inc(dims)
    underlyingCounterValue("akka_http_connections_active", dims) shouldBe 1L
  }

  it should "set connections metrics in the underlying registry" in new Fixture {
    registry.connections.inc()
    underlyingCounterValue("akka_http_connections_total") shouldBe 1L
  }

  it should "set connections metrics in the underlying registry using updated name" in new MetricsNamesFixture {
    registry.connections.inc()
    underlyingCounterValue("test_server_test_connections_total") shouldBe 1L
  }

  it should "set connections metrics in the underlying registry with dimensions" in new DimensionFixture {
    val dims = serverDimensions
    registry.connections.inc(dims)
    underlyingCounterValue("akka_http_connections_total", dims) shouldBe 1L
  }
}

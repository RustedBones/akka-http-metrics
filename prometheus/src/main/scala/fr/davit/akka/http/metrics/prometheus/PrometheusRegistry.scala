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

import fr.davit.akka.http.metrics.core._
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsSettings
import io.prometheus.client.CollectorRegistry

object PrometheusRegistry {

  private val Tolerance = 0.05

  def apply(
      settings: HttpMetricsSettings,
      underlying: CollectorRegistry = CollectorRegistry.defaultRegistry
  ): PrometheusRegistry = {
    new PrometheusRegistry(settings, underlying)
  }
}

/**
  * Prometheus registry
  * For metrics naming see [https://prometheus.io/docs/practices/naming/]
  */
class PrometheusRegistry(settings: HttpMetricsSettings, val underlying: CollectorRegistry) extends HttpMetricsRegistry {

  import PrometheusRegistry._
  import PrometheusConverters._

  private val labels: Seq[String] = {
    val statusLabel = if (settings.includeStatusDimension) Some("status") else None
    val pathLabel   = if (settings.includePathDimension) Some("path") else None

    statusLabel.toSeq ++ pathLabel
  }

  override lazy val active: Gauge = io.prometheus.client.Gauge
    .build("akka_http_requests_active", "Active HTTP requests")
    .register(underlying)

  override lazy val requests: Counter = io.prometheus.client.Counter
    .build("akka_http_requests_total", "Total HTTP requests")
    .register(underlying)

  override lazy val receivedBytes: Histogram = io.prometheus.client.Summary
    .build("akka_http_requests_size_bytes", "HTTP request size")
    .quantile(0.75, Tolerance)
    .quantile(0.95, Tolerance)
    .quantile(0.98, Tolerance)
    .quantile(0.99, Tolerance)
    .quantile(0.999, Tolerance)
    .register(underlying)

  override lazy val responses: Counter = io.prometheus.client.Counter
    .build("akka_http_responses_total", "HTTP responses")
    .labelNames(labels: _*)
    .register(underlying)

  override lazy val errors: Counter = io.prometheus.client.Counter
    .build("akka_http_responses_errors_total", "Total HTTP errors")
    .labelNames(labels: _*)
    .register(underlying)

  override lazy val duration: Timer = io.prometheus.client.Summary
    .build("akka_http_responses_duration_seconds", "HTTP response duration")
    .labelNames(labels: _*)
    .quantile(0.75, Tolerance)
    .quantile(0.95, Tolerance)
    .quantile(0.98, Tolerance)
    .quantile(0.99, Tolerance)
    .quantile(0.999, Tolerance)
    .register(underlying)

  override lazy val sentBytes: Histogram = io.prometheus.client.Summary
    .build("akka_http_responses_size_bytes", "HTTP response size")
    .labelNames(labels: _*)
    .quantile(0.75, Tolerance)
    .quantile(0.95, Tolerance)
    .quantile(0.98, Tolerance)
    .quantile(0.99, Tolerance)
    .quantile(0.999, Tolerance)
    .register(underlying)

  override val connected: Gauge = io.prometheus.client.Gauge
    .build("akka_http_connections_active", "Active TCP connections")
    .register(underlying)

  override val connections: Counter = io.prometheus.client.Counter
    .build("akka_http_connections_total", "Total TCP connections")
    .register(underlying)
}

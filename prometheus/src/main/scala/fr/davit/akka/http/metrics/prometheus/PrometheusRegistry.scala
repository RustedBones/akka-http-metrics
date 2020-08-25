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

import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{MethodDimension, PathDimension, StatusGroupDimension}
import fr.davit.akka.http.metrics.core._
import fr.davit.akka.http.metrics.prometheus.Quantiles.Quantile
import io.prometheus.client.CollectorRegistry

object PrometheusRegistry {

  implicit private class RichSummaryBuilder(val builder: io.prometheus.client.Summary.Builder) extends AnyVal {

    def quantiles(qs: Quantile*): io.prometheus.client.Summary.Builder = {
      qs.foldLeft(builder) {
        case (b, q) => b.quantile(q.percentile, q.error)
      }
    }

  }

  def apply(
      underlying: CollectorRegistry = CollectorRegistry.defaultRegistry,
      settings: PrometheusSettings = PrometheusSettings.default
  ): PrometheusRegistry = {
    new PrometheusRegistry(settings, underlying)
  }
}

/**
  * Prometheus registry
  * For metrics naming see [https://prometheus.io/docs/practices/naming/]
  */
class PrometheusRegistry(settings: PrometheusSettings, val underlying: CollectorRegistry)
    extends HttpMetricsRegistry(settings) {

  import PrometheusConverters._
  import PrometheusRegistry._

  private val labels: Seq[String] = {
    val methodLabel = if (settings.includeMethodDimension) Some(MethodDimension.Key) else None
    val pathLabel   = if (settings.includePathDimension) Some(PathDimension.Key) else None
    val statusLabel = if (settings.includeStatusDimension) Some(StatusGroupDimension.Key) else None
    (methodLabel ++ pathLabel ++ statusLabel).toSeq
  }

  override lazy val active: Gauge = io.prometheus.client.Gauge
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.activeRequests)
    .help("Active HTTP requests")
    .register(underlying)

  override lazy val requests: Counter = io.prometheus.client.Counter
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.requests)
    .help("Total HTTP requests")
    .register(underlying)

  override lazy val receivedBytes: Histogram = {
    val help = "HTTP request size"
    settings.receivedBytesConfig match {
      case Quantiles(qs, maxAge, ageBuckets) =>
        io.prometheus.client.Summary
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.requestSizes)
          .help(help)
          .quantiles(qs: _*)
          .maxAgeSeconds(maxAge.toSeconds)
          .ageBuckets(ageBuckets)
          .register(underlying)

      case Buckets(bs) =>
        io.prometheus.client.Histogram
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.requestSizes)
          .help(help)
          .buckets(bs: _*)
          .register(underlying)
    }
  }

  override lazy val responses: Counter = io.prometheus.client.Counter
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.responses)
    .help("HTTP responses")
    .labelNames(labels: _*)
    .register(underlying)

  override lazy val errors: Counter = io.prometheus.client.Counter
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.errors)
    .help("Total HTTP errors")
    .labelNames(labels: _*)
    .register(underlying)

  override lazy val duration: Timer = {
    val help = "HTTP response duration"

    settings.durationConfig match {
      case Quantiles(qs, maxAge, ageBuckets) =>
        io.prometheus.client.Summary
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.durations)
          .help(help)
          .labelNames(labels: _*)
          .quantiles(qs: _*)
          .maxAgeSeconds(maxAge.toSeconds)
          .ageBuckets(ageBuckets)
          .register(underlying)
      case Buckets(bs) =>
        io.prometheus.client.Histogram
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.durations)
          .help(help)
          .labelNames(labels: _*)
          .buckets(bs: _*)
          .register(underlying)
    }
  }

  override lazy val sentBytes: Histogram = {
    val help = "HTTP response size"

    settings.sentBytesConfig match {
      case Quantiles(qs, maxAge, ageBuckets) =>
        io.prometheus.client.Summary
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.responseSizes)
          .help(help)
          .labelNames(labels: _*)
          .quantiles(qs: _*)
          .maxAgeSeconds(maxAge.toSeconds)
          .ageBuckets(ageBuckets)
          .register(underlying)

      case Buckets(bs) =>
        io.prometheus.client.Histogram
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.responseSizes)
          .help(help)
          .labelNames(labels: _*)
          .buckets(bs: _*)
          .register(underlying)
    }
  }

  override val connected: Gauge = io.prometheus.client.Gauge
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.activeConnections)
    .help("Active TCP connections")
    .register(underlying)

  override val connections: Counter = io.prometheus.client.Counter
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.connections)
    .help("Total TCP connections")
    .register(underlying)
}

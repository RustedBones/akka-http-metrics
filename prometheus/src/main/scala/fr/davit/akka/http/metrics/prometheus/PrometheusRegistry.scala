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
import fr.davit.akka.http.metrics.prometheus.Quantiles.Quantile
import io.prometheus.client.CollectorRegistry

object PrometheusRegistry {

  implicit private class RichSummaryBuilder(val builder: io.prometheus.client.Summary.Builder) extends AnyVal {

    def quantiles(qs: Quantile*): io.prometheus.client.Summary.Builder = {
      qs.foldLeft(builder) { case (b, q) =>
        b.quantile(q.percentile, q.error)
      }
    }

  }

  def apply(
      underlying: CollectorRegistry = CollectorRegistry.defaultRegistry,
      settings: PrometheusSettings = PrometheusSettings.default
  ): PrometheusRegistry = {
    new PrometheusRegistry(settings, underlying)
  }

  // order dimension by names
  // this helps making sure same dimension are given in the same order
  // when creating collectors and observing metrics
  implicit val DimensionOrdering: Ordering[Dimension] = Ordering.by(_.name)
}

/** Prometheus registry For metrics naming see [https://prometheus.io/docs/practices/naming/]
  */
class PrometheusRegistry(settings: PrometheusSettings, val underlying: CollectorRegistry)
    extends HttpMetricsRegistry(settings) {

  import PrometheusConverters._
  import PrometheusRegistry._

  private val methodDimension = if (settings.includeMethodDimension) Some(MethodLabeler.name) else None
  private val pathDimension   = if (settings.includePathDimension) Some(PathLabeler.name) else None
  private val statusDimension = if (settings.includeStatusDimension) Some(StatusGroupLabeler.name) else None

  private[prometheus] val serverDimensions = settings.serverDimensions.map(_.name)

  private val customRequestDimensions = settings.customDimensions.collect { case l: HttpRequestLabeler => l.name }
  private[prometheus] val requestsDimensions = (methodDimension ++ customRequestDimensions).toSeq

  private val customResponseDimensions = settings.customDimensions.collect { case l: HttpResponseLabeler => l.name }
  private[prometheus] val responsesDimensions = (statusDimension ++ pathDimension ++ customResponseDimensions).toSeq

  lazy val requests: Counter = io.prometheus.client.Counter
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.requests)
    .help("Total HTTP requests")
    .labelNames(serverDimensions ++ requestsDimensions: _*)
    .register(underlying)

  lazy val requestsActive: Gauge = io.prometheus.client.Gauge
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.requestsActive)
    .help("Active HTTP requests")
    .labelNames(serverDimensions ++ requestsDimensions: _*)
    .register(underlying)

  lazy val requestsFailures: Counter = io.prometheus.client.Counter
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.requestsFailures)
    .help("Total unserved requests")
    .labelNames(serverDimensions ++ requestsDimensions: _*)
    .register(underlying)

  lazy val requestsSize: Histogram = {
    val help = "HTTP request size"
    settings.receivedBytesConfig match {
      case Quantiles(qs, maxAge, ageBuckets) =>
        io.prometheus.client.Summary
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.requestsSize)
          .help(help)
          .labelNames(serverDimensions ++ requestsDimensions: _*)
          .quantiles(qs: _*)
          .maxAgeSeconds(maxAge.toSeconds)
          .ageBuckets(ageBuckets)
          .register(underlying)

      case Buckets(bs) =>
        io.prometheus.client.Histogram
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.requestsSize)
          .help(help)
          .labelNames(serverDimensions ++ requestsDimensions: _*)
          .buckets(bs: _*)
          .register(underlying)
    }
  }

  lazy val responses: Counter = io.prometheus.client.Counter
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.responses)
    .help("HTTP responses")
    .labelNames(serverDimensions ++ requestsDimensions ++ responsesDimensions: _*)
    .register(underlying)

  lazy val responsesErrors: Counter = io.prometheus.client.Counter
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.responsesErrors)
    .help("Total HTTP errors")
    .labelNames(serverDimensions ++ requestsDimensions ++ responsesDimensions: _*)
    .register(underlying)

  lazy val responsesDuration: Timer = {
    val help = "HTTP response duration"

    settings.durationConfig match {
      case Quantiles(qs, maxAge, ageBuckets) =>
        io.prometheus.client.Summary
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.responsesDuration)
          .help(help)
          .labelNames(serverDimensions ++ requestsDimensions ++ responsesDimensions: _*)
          .quantiles(qs: _*)
          .maxAgeSeconds(maxAge.toSeconds)
          .ageBuckets(ageBuckets)
          .register(underlying)
      case Buckets(bs) =>
        io.prometheus.client.Histogram
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.responsesDuration)
          .help(help)
          .labelNames(serverDimensions ++ requestsDimensions ++ responsesDimensions: _*)
          .buckets(bs: _*)
          .register(underlying)
    }
  }

  lazy val responsesSize: Histogram = {
    val help = "HTTP response size"

    settings.sentBytesConfig match {
      case Quantiles(qs, maxAge, ageBuckets) =>
        io.prometheus.client.Summary
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.responsesSize)
          .help(help)
          .labelNames(serverDimensions ++ requestsDimensions ++ responsesDimensions: _*)
          .quantiles(qs: _*)
          .maxAgeSeconds(maxAge.toSeconds)
          .ageBuckets(ageBuckets)
          .register(underlying)

      case Buckets(bs) =>
        io.prometheus.client.Histogram
          .build()
          .namespace(settings.namespace)
          .name(settings.metricsNames.responsesSize)
          .help(help)
          .labelNames(serverDimensions ++ requestsDimensions ++ responsesDimensions: _*)
          .buckets(bs: _*)
          .register(underlying)
    }
  }

  lazy val connections: Counter = io.prometheus.client.Counter
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.connections)
    .help("Total TCP connections")
    .labelNames(serverDimensions: _*)
    .register(underlying)

  lazy val connectionsActive: Gauge = io.prometheus.client.Gauge
    .build()
    .namespace(settings.namespace)
    .name(settings.metricsNames.connectionsActive)
    .help("Active TCP connections")
    .labelNames(serverDimensions: _*)
    .register(underlying)
}

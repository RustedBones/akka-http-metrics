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

import scala.concurrent.duration.FiniteDuration

object PrometheusRegistry {

  private val AkkaPrefix = "akka_http"

  private val Tolerance = 0.05

  private def name(names: String*): String = (AkkaPrefix +: names).mkString("_")

  private implicit def toLongCounter(counter: io.prometheus.client.Counter): Counter[Long] = new Counter[Long] {
    override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
      counter.labels(dimensions.map(_.value): _*).inc()
    }
  }

  private implicit def toLongGauge(gauge: io.prometheus.client.Gauge): Gauge[Long] = new Gauge[Long] {
    override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
      gauge.labels(dimensions.map(_.value): _*).inc()
    }

    override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
      gauge.labels(dimensions.map(_.value): _*).dec()
    }
  }

  private implicit def toTimer(summary: io.prometheus.client.Summary): Timer = new Timer {
    override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
      summary.labels(dimensions.map(_.value): _*).observe(duration.toMillis.toDouble / 1000.0)
    }
  }

  private implicit def toLongHistogram(summary: io.prometheus.client.Summary): Histogram[Long] = new Histogram[Long] {
    override def update(value: Long, dimensions: Seq[Dimension] = Seq.empty): Unit = {
      summary.labels(dimensions.map(_.value): _*).observe(value.toDouble)
    }
  }

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

  private val labels: Seq[String] = {
    val statusLabel = if (settings.includeStatusDimension) Some("status") else None
    val pathLabel   = if (settings.includePathDimension) Some("path") else None

    statusLabel.toSeq ++ pathLabel
  }

  override val active: Gauge[Long] = io.prometheus.client.Gauge
    .build(name("requests", "active"), "Active HTTP requests")
    .register(underlying)

  override val requests: Counter[Long] = io.prometheus.client.Counter
    .build(name("requests", "total"), "Total HTTP requests")
    .register(underlying)

  override val receivedBytes: Histogram[Long] = io.prometheus.client.Summary
    .build(name("requests", "size", "bytes"), "HTTP request size")
    .quantile(0.75, Tolerance)
    .quantile(0.95, Tolerance)
    .quantile(0.98, Tolerance)
    .quantile(0.99, Tolerance)
    .quantile(0.999, Tolerance)
    .register(underlying)

  override val responses: Counter[Long] = io.prometheus.client.Counter
    .build(name("responses", "total"), "HTTP responses")
    .labelNames(labels: _*)
    .register(underlying)

  override val errors: Counter[Long] = io.prometheus.client.Counter
    .build(name("responses", "errors", "total"), "Total HTTP errors")
    .labelNames(labels: _*)
    .register(underlying)

  override val duration: Timer = io.prometheus.client.Summary
    .build(name("responses", "duration", "seconds"), "HTTP response duration")
    .labelNames(labels: _*)
    .quantile(0.75, Tolerance)
    .quantile(0.95, Tolerance)
    .quantile(0.98, Tolerance)
    .quantile(0.99, Tolerance)
    .quantile(0.999, Tolerance)
    .register(underlying)

  override val sentBytes: Histogram[Long] = io.prometheus.client.Summary
    .build(name("responses", "size", "bytes"), "HTTP response size")
    .labelNames(labels: _*)
    .quantile(0.75, Tolerance)
    .quantile(0.95, Tolerance)
    .quantile(0.98, Tolerance)
    .quantile(0.99, Tolerance)
    .quantile(0.999, Tolerance)
    .register(underlying)

  override val connected: Gauge[Long] = io.prometheus.client.Gauge
    .build(name("connections", "active"), "Active TCP connections")
    .register(underlying)

  override val connections: Counter[Long] = io.prometheus.client.Counter
    .build(name("connections", "total"), "Total TCP connections")
    .register(underlying)
}

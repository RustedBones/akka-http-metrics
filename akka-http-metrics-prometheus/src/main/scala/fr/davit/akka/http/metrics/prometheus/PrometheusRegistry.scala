package fr.davit.akka.http.metrics.prometheus

import fr.davit.akka.http.metrics.core.{Counter, Gauge, Histogram, HttpMetricsRegistry, Timer}
import io.prometheus.client.CollectorRegistry

import scala.concurrent.duration.FiniteDuration

object PrometheusRegistry {

  private val AkkaPrefix = "akka_http"

  private val Tolerance = 0.05

  private def name(names: String*): String = (AkkaPrefix +: names).mkString("_")

  private implicit def toLongCounter(counter: io.prometheus.client.Counter): Counter[Long] = new Counter[Long] {
    override def inc(): Unit = counter.inc()
  }

  private implicit def toLongGauge(gauge: io.prometheus.client.Gauge): Gauge[Long] = new Gauge[Long] {
    override def inc(): Unit = gauge.inc()

    override def dec(): Unit = gauge.dec()
  }

  private implicit def toTimer(summary: io.prometheus.client.Summary): Timer = new Timer {
    override def observe(duration: FiniteDuration): Unit = summary.observe(duration.toMillis / 1000.0)
  }

  private implicit def toLongHistogram(summary: io.prometheus.client.Summary): Histogram[Long] = new Histogram[Long] {
    override def update(value: Long): Unit = summary.observe(value.toDouble)
  }

  def apply(underlying: CollectorRegistry = CollectorRegistry.defaultRegistry): PrometheusRegistry = {
    new PrometheusRegistry(underlying)
  }
}


/**
  * Prometheus registry
  * For metrics naming see [https://prometheus.io/docs/practices/naming/]
  */
class PrometheusRegistry(val underlying: CollectorRegistry) extends HttpMetricsRegistry {

  import PrometheusRegistry._

  override val requests: Counter[Long] = io.prometheus.client.Counter
    .build(name("requests", "total"), "Total HTTP requests")
    .register(underlying)

  override val errors: Counter[Long] = io.prometheus.client.Counter
    .build(name("requests", "errors", "total"), "Total HTTP errors")
    .register(underlying)

  override val active: Gauge[Long] = io.prometheus.client.Gauge
    .build(name("requests", "active"), "Active HTTP requests")
    .register(underlying)

  override val duration: Timer = io.prometheus.client.Summary
    .build(name("requests", "duration", "seconds"), "HTTP request duration")
    .quantile(0.75, Tolerance)
    .quantile(0.95, Tolerance)
    .quantile(0.98, Tolerance)
    .quantile(0.99, Tolerance)
    .quantile(0.999, Tolerance)
    .register(underlying)

  override val receivedBytes: Histogram[Long] = io.prometheus.client.Summary
    .build(name("requests", "size", "bytes"), "HTTP request size")
    .quantile(0.75, Tolerance)
    .quantile(0.95, Tolerance)
    .quantile(0.98, Tolerance)
    .quantile(0.99, Tolerance)
    .quantile(0.999, Tolerance)
    .register(underlying)

  override val sentBytes: Histogram[Long] = io.prometheus.client.Summary
    .build(name("responses", "size", "bytes"), "HTTP response size")
    .quantile(0.75, Tolerance)
    .quantile(0.95, Tolerance)
    .quantile(0.98, Tolerance)
    .quantile(0.99, Tolerance)
    .quantile(0.999, Tolerance)
    .register(underlying)
}

package fr.davit.akka.http.metrics.prometheus

import fr.davit.akka.http.metrics.core._

import scala.concurrent.duration.FiniteDuration

class PrometheusCounter(counter: io.prometheus.client.Counter) extends Counter {
  override def inc(dimensions: Seq[Dimension]): Unit = {
    counter.labels(dimensions.map(_.value): _*).inc()
  }
}

class PrometheusGauge(gauge: io.prometheus.client.Gauge) extends Gauge {
  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    gauge.labels(dimensions.map(_.value): _*).inc()
  }

  override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    gauge.labels(dimensions.map(_.value): _*).dec()
  }
}

class PrometheusTimer(summary: io.prometheus.client.Summary) extends Timer {
  override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
    summary.labels(dimensions.map(_.value): _*).observe(duration.toMillis.toDouble / 1000.0)
  }
}

class PrometheusSummary(summary: io.prometheus.client.Summary) extends Histogram {
  override def update[T](value: T, dimensions: Seq[Dimension] = Seq.empty)(implicit numeric: Numeric[T]): Unit = {
    summary.labels(dimensions.map(_.value): _*).observe(numeric.toDouble(value))
  }
}

class PrometheusHistogram(histogram: io.prometheus.client.Histogram) extends Histogram {
  override def update[T](value: T, dimensions: Seq[Dimension] = Seq.empty)(implicit numeric: Numeric[T]): Unit = {
    histogram.labels(dimensions.map(_.value): _*).observe(numeric.toDouble(value))
  }
}

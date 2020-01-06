package fr.davit.akka.http.metrics.graphite

import fr.davit.akka.http.metrics.core.{Counter, Dimension, Gauge, Histogram, Timer}

import scala.concurrent.duration.FiniteDuration

object CarbonMetrics {

  def name(name: String, dimensions: Seq[Dimension]): String = {
    val tags = dimensions.map(d => d.key + "=" + d.value).toList
    (name :: tags).mkString(";")
  }
}

class CarbonCounter(name: String)(implicit client: CarbonClient) extends Counter {
  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(CarbonMetrics.name(name, dimensions), 1)
  }
}

class CarbonGauge(name: String)(implicit client: CarbonClient) extends Gauge {
  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(CarbonMetrics.name(name, dimensions), 1)
  }

  override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(CarbonMetrics.name(name, dimensions), -1)
  }
}

class CarbonTimer(name: String)(implicit client: CarbonClient) extends Timer {
  override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(CarbonMetrics.name(name, dimensions), duration.toMillis)
  }
}

class CarbonHistogram(name: String)(implicit client: CarbonClient) extends Histogram {
  override def update[T: Numeric](value: T, dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(CarbonMetrics.name(name, dimensions), value)
  }
}

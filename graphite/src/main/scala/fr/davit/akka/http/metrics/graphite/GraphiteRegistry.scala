package fr.davit.akka.http.metrics.graphite

import fr.davit.akka.http.metrics.core.{Counter, Dimension, Gauge, Histogram, HttpMetricsRegistry, Timer}

import scala.concurrent.duration.FiniteDuration

object GraphiteRegistry {

  val AkkaPrefix = "akka.http."

  private implicit class RichCarbonClient(client: CarbonClient) {

    private def metricName(name: String, dimensions: Seq[Dimension]): String = {
      val tags = dimensions.map(d => d.key + "=" + d.value).toList
      (AkkaPrefix + name :: tags).mkString(";")
    }

    def counter[T](name: String): Counter[T] = new Counter[T] {
      override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.publish(metricName(name, dimensions), 1)
      }
    }

    def gauge[T](name: String): Gauge[T] = new Gauge[T] {
      override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.publish(metricName(name, dimensions), 1)
      }

      override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.publish(metricName(name, dimensions), -1)
      }
    }

    def timer(name: String): Timer = new Timer {
      override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.publish(metricName(name, dimensions), duration.toMillis)
      }
    }

    def histogram[T](name: String): Histogram[T] = new Histogram[T] {
      override def update(value: T, dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.publish(metricName(name, dimensions), value)
      }
    }
  }

  def apply(client: CarbonClient): GraphiteRegistry = new GraphiteRegistry(client)
}

class GraphiteRegistry(client: CarbonClient) extends HttpMetricsRegistry {

  import GraphiteRegistry._

  override def active: Gauge[Long] = client.gauge("requests.active")

  override def requests: Counter[Long] = client.counter("requests")

  override def receivedBytes: Histogram[Long] = client.histogram("requests.bytes")

  override def responses: Counter[Long] = client.counter("responses")

  override def errors: Counter[Long] = client.counter("responses.errors")

  override def duration: Timer = client.timer("responses.duration")

  override def sentBytes: Histogram[Long] = client.histogram("responses.bytes")

  override def connected: Gauge[Long] = client.gauge("connections.active")

  override def connections: Counter[Long] = client.counter("connections")
}

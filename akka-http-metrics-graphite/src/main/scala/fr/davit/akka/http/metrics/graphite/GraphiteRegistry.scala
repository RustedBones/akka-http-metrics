package fr.davit.akka.http.metrics.graphite

import fr.davit.akka.http.metrics.core.{Counter, Dimension, Gauge, Histogram, HttpMetricsRegistry, Timer}

import scala.concurrent.duration.FiniteDuration

object GraphiteRegistry {


  private implicit class RichCarbonClient(client: CarbonClient) {
    def counter[T](name: String): Counter[T] = new Counter[T] {
      override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.publish(name, 1, dimensions: _*)
      }
    }

    def gauge[T](name: String): Gauge[T] = new Gauge[T] {
      override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.publish(name, 1, dimensions: _*)
      }

      override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.publish(name, -1, dimensions: _*)
      }
    }

    def timer(name: String): Timer = new Timer {
      override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.publish(name, duration.toMillis, dimensions: _*)
      }
    }

    def histogram[T](name: String): Histogram[T] = new Histogram[T] {
      override def update(value: T, dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.publish(name, value, dimensions: _*)
      }
    }
  }
}

class GraphiteRegistry(client: CarbonClient) extends HttpMetricsRegistry {

  import GraphiteRegistry._

  override def active: Gauge[Long] = client.gauge("")

  override def requests: Counter[Long] = client.counter("")

  override def receivedBytes: Histogram[Long] = client.histogram("")

  override def responses: Counter[Long] = client.counter("")

  override def errors: Counter[Long] = client.counter("")

  override def duration: Timer = client.timer("")

  override def sentBytes: Histogram[Long] = client.histogram("")

  override def connected: Gauge[Long] = client.gauge("")

  override def connections: Counter[Long] = client.counter("")
}

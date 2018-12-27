package fr.davit.akka.http.metrics.datadog

import com.timgroup.statsd.StatsDClient
import fr.davit.akka.http.metrics.core._

import scala.concurrent.duration.FiniteDuration

object DatadogRegistry {

  val AkkaPrefix = "akka.http"

  def name(names: String*): String = s"$AkkaPrefix.${names.mkString("_")}"

  private class RichStatsDClient(client: StatsDClient) {
    def longCounter(name: String): Counter[Long] = new Counter[Long] {
      override def inc(): Unit = client.increment(name)

      override def value: Long = ???
    }

    def longGauge(name: String): Gauge[Long] = new Gauge[Long] {
      override def inc(): Unit = client.increment(name)

      override def dec(): Unit = client.decrement(name)

      override def value: Long = ???
    }

    def timer(name: String): Timer = new Timer {
      override def observe(duration: FiniteDuration): Unit = client.distribution(name, duration.toMillis)
    }

    def longHistogram(name: String): Histogram[Long] = new Histogram[Long] {
      override def update(value: Long): Unit = client.distribution(name, value)
    }
  }

  private implicit def enrichClient(client: StatsDClient): RichStatsDClient = new RichStatsDClient(client)

  def apply(client: StatsDClient): DatadogRegistry = new DatadogRegistry(client)
}

/**
  * see [https://docs.datadoghq.com/developers/faq/what-best-practices-are-recommended-for-naming-metrics-and-tags/]
  * @param client
  */
class DatadogRegistry(client: StatsDClient) extends HttpMetricsRegistry {

  import DatadogRegistry._

  override val requests: Counter[Long] = client.longCounter(name("requests", "count"))

  override val errors: Counter[Long] = client.longCounter(name("requests", "errors", "count"))

  override val active: Gauge[Long] = client.longGauge(name("requests", "active"))

  override val duration: Timer = client.timer(name("requests", "duration"))

  override val receivedBytes: Histogram[Long] = client.longHistogram(name("requests", "bytes"))

  override val sentBytes: Histogram[Long] = client.longHistogram(name("responses", "bytes") )
}

package fr.davit.akka.http.metrics.dropwizard

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.MetricRegistry.name
import fr.davit.akka.http.metrics.core._

import scala.concurrent.duration.FiniteDuration

object DropwizardRegistry {

  val AkkaPrefix = "akka.http"

  private implicit def toLongCounter(counter: com.codahale.metrics.Counter): Counter[Long] = new Counter[Long] {
    override def inc(): Unit = counter.inc()

    override def value: Long = counter.getCount
  }

  private implicit def toLongGauge(counter: com.codahale.metrics.Counter): Gauge[Long] = new Gauge[Long] {
    override def inc(): Unit = counter.inc()

    override def dec(): Unit = counter.dec()

    override def value: Long = counter.getCount
  }

  private implicit def toTimer(timer: com.codahale.metrics.Timer): Timer = new Timer {
    override def observe(duration: FiniteDuration): Unit = timer.update(duration.length, duration.unit)
  }

  private implicit def toLongHistogram(histogram: com.codahale.metrics.Histogram): Histogram[Long] = new Histogram[Long] {
    override def update(value: Long): Unit = histogram.update(value)
  }

  def apply(registry: MetricRegistry = new MetricRegistry()): DropwizardRegistry = {
    new DropwizardRegistry(registry)
  }
}

class DropwizardRegistry(val underlying: MetricRegistry) extends HttpMetricsRegistry {

  import DropwizardRegistry._

  override val requests: Counter[Long] = underlying.counter(name(AkkaPrefix, "requests"))

  override val errors: Counter[Long] = underlying.counter(name(AkkaPrefix, "requests", "errors"))

  override val active: Gauge[Long] = underlying.counter(name(AkkaPrefix,"requests", "active"))

  override val duration: Timer = underlying.timer(name(AkkaPrefix, "requests", "durations"))

  override val receivedBytes: Histogram[Long] = underlying.histogram(name(AkkaPrefix, "requests", "sizes"))

  override val sentBytes: Histogram[Long] = underlying.histogram(name(AkkaPrefix, "responses", "sizes"))
}

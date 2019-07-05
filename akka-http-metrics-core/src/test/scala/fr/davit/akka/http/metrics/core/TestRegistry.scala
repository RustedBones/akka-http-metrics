package fr.davit.akka.http.metrics.core

import java.util.concurrent.atomic.LongAdder

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.HttpEntity

import scala.concurrent.duration.FiniteDuration

object TestRegistry {
  implicit val marshaller: ToEntityMarshaller[TestRegistry] = Marshaller.opaque(_ => HttpEntity.Empty)

  final class TestCounter extends Counter[Long] {
    private val acc = new LongAdder()

    override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = acc.increment()

    def value: Long = acc.longValue()
  }

  final class TestGauge extends Gauge[Long] {
    private val acc = new LongAdder()

    override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = acc.increment()

    override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = acc.decrement()

    def value: Long = acc.longValue()
  }

  final class TestTimer extends Timer {
    private val builder = Seq.newBuilder[FiniteDuration]

    override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = builder += duration

    def values: Seq[FiniteDuration] = builder.result()
  }

  final class TestHistogram extends Histogram[Long] {
    private val builder = Seq.newBuilder[Long]

    override def update(value: Long, dimensions: Seq[Dimension] = Seq.empty): Unit = builder += value

    def values: Seq[Long] = builder.result()
  }

}

final class TestRegistry extends HttpMetricsRegistry {

  import TestRegistry._

  override val active = new TestGauge

  override val requests = new TestCounter

  override val receivedBytes = new TestHistogram

  override val responses = new TestCounter

  override val errors = new TestCounter

  override val duration = new TestTimer

  override val sentBytes = new TestHistogram

  override val connected = new TestGauge

  override val connections = new TestCounter
}

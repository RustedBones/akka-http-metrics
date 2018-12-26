package fr.davit.akka.http.metrics.core

import scala.concurrent.duration.FiniteDuration

sealed trait ScalarMetric[T] {
  def value: T
}

trait Counter[T] extends ScalarMetric[T] {
  def inc(): Unit
}

trait Gauge[T] extends ScalarMetric[T] {
  def inc(): Unit

  def dec(): Unit
}

trait Timer {
  def observe(duration: FiniteDuration): Unit
}

trait Histogram[T] {
  def update(value: T): Unit
}

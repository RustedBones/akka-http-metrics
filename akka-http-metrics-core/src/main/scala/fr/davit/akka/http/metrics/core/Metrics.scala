package fr.davit.akka.http.metrics.core

import scala.concurrent.duration.FiniteDuration

trait Counter[T] {
  def inc(): Unit
}

trait Gauge[T] {
  def inc(): Unit

  def dec(): Unit
}

trait Timer {
  def observe(duration: FiniteDuration): Unit
}

trait Histogram[T] {
  def update(value: T): Unit
}

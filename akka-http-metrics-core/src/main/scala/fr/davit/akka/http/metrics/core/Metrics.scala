package fr.davit.akka.http.metrics.core

import scala.concurrent.duration.FiniteDuration

trait Dimension {
  def key: String
  def value: String
}

trait Counter[T] {
  def inc(dimensions: Seq[Dimension] = Seq.empty): Unit
}

trait Gauge[T] {
  def inc(dimensions: Seq[Dimension] = Seq.empty): Unit

  def dec(dimensions: Seq[Dimension] = Seq.empty): Unit
}

trait Timer {
  def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit
}

trait Histogram[T] {
  def update(value: T, dimensions: Seq[Dimension] = Seq.empty): Unit
}

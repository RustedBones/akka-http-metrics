package fr.davit.akka.http.metrics.core

trait HttpMetricsRegistry {

  def requests: Counter[Long]

  def errors: Counter[Long]

  def active: Gauge[Long]

  def duration: Timer

  def receivedBytes: Histogram[Long]

  def sentBytes: Histogram[Long]
}
package fr.davit.akka.http.metrics.graphite

import fr.davit.akka.http.metrics.core.{Counter, Gauge, Histogram, HttpMetricsRegistry, Timer}

class GraphiteRegistry extends HttpMetricsRegistry {

  override def active: Gauge[Long] = ???

  override def requests: Counter[Long] = ???

  override def receivedBytes: Histogram[Long] = ???

  override def responses: Counter[Long] = ???

  override def errors: Counter[Long] = ???

  override def duration: Timer = ???

  override def sentBytes: Histogram[Long] = ???

  override def connected: Gauge[Long] = ???

  override def connections: Counter[Long] = ???
}

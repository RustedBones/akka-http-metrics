/*
 * Copyright 2019 Michel Davit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.davit.akka.http.metrics.dropwizard

import fr.davit.akka.http.metrics.core._
import io.dropwizard.metrics5.{MetricName, MetricRegistry}

import scala.concurrent.duration.FiniteDuration

object DropwizardRegistry {

  private val AkkaPrefix = Seq("akka", "http")

  private implicit class RichMetricsRegistry(underlying: MetricRegistry) {

    private def metricName(name: Seq[String], dimensions: Seq[Dimension]): MetricName = {
      MetricName.build(AkkaPrefix ++ name: _*).tagged(dimensions.flatMap(d => Seq(d.key, d.value)): _*)
    }

    def longCounter(name: String*): Counter[Long] = new Counter[Long] {
      override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        underlying.counter(metricName(name, dimensions)).inc()
      }
    }

    def longGauge(name: String*): Gauge[Long] = new Gauge[Long] {
      override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        underlying.counter(metricName(name, dimensions)).inc()
      }

      override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        underlying.counter(metricName(name, dimensions)).dec()
      }
    }

    def customTimer(name: String*): Timer = new Timer {
      override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
        underlying.timer(metricName(name, dimensions)).update(duration.length, duration.unit)
      }
    }

    def longHistogram(name: String*): Histogram[Long] = new Histogram[Long] {
      override def update(value: Long, dimensions: Seq[Dimension] = Seq.empty): Unit = {
        underlying.histogram(metricName(name, dimensions)).update(value)
      }
    }
  }

  def apply(registry: MetricRegistry = new MetricRegistry()): DropwizardRegistry = {
    new DropwizardRegistry(registry)
  }
}

class DropwizardRegistry(val underlying: MetricRegistry) extends HttpMetricsRegistry {

  import DropwizardRegistry._

  override val active: Gauge[Long] = underlying.longGauge("requests", "active")

  override val requests: Counter[Long] = underlying.longCounter("requests")

  override val receivedBytes: Histogram[Long] = underlying.longHistogram("requests", "bytes")

  override val responses: Counter[Long] = underlying.longCounter("responses")

  override val errors: Counter[Long] = underlying.longCounter("responses", "errors")

  override val duration: Timer = underlying.customTimer("responses", "duration")

  override val sentBytes: Histogram[Long] = underlying.longHistogram("responses", "bytes")

  override val connected: Gauge[Long] = underlying.longGauge("connections", "active")

  override val connections: Counter[Long] = underlying.longCounter("connections")
}

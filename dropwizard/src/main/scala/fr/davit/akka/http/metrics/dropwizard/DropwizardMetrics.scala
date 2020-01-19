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

import fr.davit.akka.http.metrics.core.{Counter, Dimension, Gauge, Histogram, Timer}
import io.dropwizard.metrics5.{MetricName, MetricRegistry}

import scala.concurrent.duration.FiniteDuration

object DropwizardMetrics {

  def name(name: String, dimensions: Seq[Dimension]): MetricName = {
    MetricName.build(name).tagged(dimensions.flatMap(d => Seq(d.key, d.value)): _*)
  }
}

class DropwizardCounter(name: String)(implicit registry: MetricRegistry) extends Counter {
  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    registry.counter(DropwizardMetrics.name(name, dimensions)).inc()
  }
}

class DropwizardGauge(name: String)(implicit registry: MetricRegistry) extends Gauge {
  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    registry.counter(DropwizardMetrics.name(name, dimensions)).inc()
  }

  override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    registry.counter(DropwizardMetrics.name(name, dimensions)).dec()
  }
}

class DropwizardTimer(name: String)(implicit registry: MetricRegistry) extends Timer {
  override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
    registry.timer(DropwizardMetrics.name(name, dimensions)).update(duration.length, duration.unit)
  }
}

class DropwizardHistogram(name: String)(implicit registry: MetricRegistry) extends Histogram {
  override def update[T](value: T, dimensions: Seq[Dimension] = Seq.empty)(implicit numeric: Numeric[T]): Unit = {
    registry.histogram(DropwizardMetrics.name(name, dimensions)).update(numeric.toLong(value))
  }
}

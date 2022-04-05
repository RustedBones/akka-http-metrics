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

  implicit class RichMetricsName(val metricName: MetricName) extends AnyVal {

    def tagged(dimensions: Seq[Dimension]): MetricName =
      metricName.tagged(dimensions.flatMap(d => Seq(d.name, d.label)): _*)

  }
}

abstract class DropwizardMetrics(namespace: String, name: String) {
  protected lazy val metricName: MetricName = MetricName.build(namespace, name)
}

class DropwizardCounter(namespace: String, name: String)(implicit registry: MetricRegistry)
    extends DropwizardMetrics(namespace, name)
    with Counter {

  import DropwizardMetrics._

  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    registry.counter(metricName.tagged(dimensions)).inc()
  }
}

class DropwizardGauge(namespace: String, name: String)(implicit registry: MetricRegistry)
    extends DropwizardMetrics(namespace, name)
    with Gauge {

  import DropwizardMetrics._

  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    registry.counter(metricName.tagged(dimensions)).inc()
  }

  override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    registry.counter(metricName.tagged(dimensions)).dec()
  }
}

class DropwizardTimer(namespace: String, name: String)(implicit registry: MetricRegistry)
    extends DropwizardMetrics(namespace, name)
    with Timer {

  import DropwizardMetrics._

  override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
    registry.timer(metricName.tagged(dimensions)).update(duration.length, duration.unit)
  }
}

class DropwizardHistogram(namespace: String, name: String)(implicit registry: MetricRegistry)
    extends DropwizardMetrics(namespace, name)
    with Histogram {

  import DropwizardMetrics._

  override def update[T](value: T, dimensions: Seq[Dimension] = Seq.empty)(implicit numeric: Numeric[T]): Unit = {
    registry.histogram(metricName.tagged(dimensions)).update(numeric.toLong(value))
  }
}

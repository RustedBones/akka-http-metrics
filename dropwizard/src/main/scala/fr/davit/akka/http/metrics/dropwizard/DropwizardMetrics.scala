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
import com.codahale.metrics.MetricRegistry

import scala.concurrent.duration.FiniteDuration

abstract class DropwizardMetrics(namespace: String, name: String) {
  private val baseName: String = MetricRegistry.name(namespace, name)

  def metricsNames(dimensions: Seq[Dimension]): Seq[String] = {
    if (dimensions.isEmpty) {
      Seq(baseName)
    } else {
      dimensions.map(d => MetricRegistry.name(baseName, s"${d.value}-${d.key}"))
    }
  }
}

class DropwizardCounter(namespace: String, name: String)(implicit registry: MetricRegistry)
    extends DropwizardMetrics(namespace, name)
    with Counter {

  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    metricsNames(dimensions).map(registry.counter).foreach(_.inc())
  }
}

class DropwizardGauge(namespace: String, name: String)(implicit registry: MetricRegistry)
    extends DropwizardMetrics(namespace, name)
    with Gauge {

  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    metricsNames(dimensions).map(registry.counter).foreach(_.inc())
  }

  override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    metricsNames(dimensions).map(registry.counter).foreach(_.dec())
  }
}

class DropwizardTimer(namespace: String, name: String)(implicit registry: MetricRegistry)
    extends DropwizardMetrics(namespace, name)
    with Timer {

  override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
    metricsNames(dimensions).map(registry.timer).foreach(_.update(duration.length, duration.unit))
  }
}

class DropwizardHistogram(namespace: String, name: String)(implicit registry: MetricRegistry)
    extends DropwizardMetrics(namespace, name)
    with Histogram {

  override def update[T](value: T, dimensions: Seq[Dimension] = Seq.empty)(implicit numeric: Numeric[T]): Unit = {
    metricsNames(dimensions).map(registry.histogram).foreach(_.update(numeric.toLong(value)))
  }
}

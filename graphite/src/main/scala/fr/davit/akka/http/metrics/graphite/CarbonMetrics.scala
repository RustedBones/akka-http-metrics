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

package fr.davit.akka.http.metrics.graphite

import fr.davit.akka.http.metrics.core.{Counter, Dimension, Gauge, Histogram, Timer}

import scala.concurrent.duration.FiniteDuration

abstract class CarbonMetrics(namespace: String, name: String) {
  protected lazy val metricName: String = s"$namespace.$name"
}

class CarbonCounter(namespace: String, name: String)(implicit client: CarbonClient)
    extends CarbonMetrics(namespace, name)
    with Counter {
  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(metricName, 1, dimensions)
  }
}

class CarbonGauge(namespace: String, name: String)(implicit client: CarbonClient)
    extends CarbonMetrics(namespace, name)
    with Gauge {
  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(metricName, 1, dimensions)
  }

  override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(metricName, -1, dimensions)
  }
}

class CarbonTimer(namespace: String, name: String)(implicit client: CarbonClient)
    extends CarbonMetrics(namespace, name)
    with Timer {
  override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(metricName, duration.toMillis, dimensions)
  }
}

class CarbonHistogram(namespace: String, name: String)(implicit client: CarbonClient)
    extends CarbonMetrics(namespace, name)
    with Histogram {
  override def update[T: Numeric](value: T, dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(metricName, value, dimensions)
  }
}

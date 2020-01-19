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

object CarbonMetrics {

  def name(name: String, dimensions: Seq[Dimension]): String = {
    val tags = dimensions.map(d => d.key + "=" + d.value).toList
    (name :: tags).mkString(";")
  }
}

class CarbonCounter(name: String)(implicit client: CarbonClient) extends Counter {
  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(CarbonMetrics.name(name, dimensions), 1)
  }
}

class CarbonGauge(name: String)(implicit client: CarbonClient) extends Gauge {
  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(CarbonMetrics.name(name, dimensions), 1)
  }

  override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(CarbonMetrics.name(name, dimensions), -1)
  }
}

class CarbonTimer(name: String)(implicit client: CarbonClient) extends Timer {
  override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(CarbonMetrics.name(name, dimensions), duration.toMillis)
  }
}

class CarbonHistogram(name: String)(implicit client: CarbonClient) extends Histogram {
  override def update[T: Numeric](value: T, dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.publish(CarbonMetrics.name(name, dimensions), value)
  }
}

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

package fr.davit.akka.http.metrics.datadog

import com.timgroup.statsd.StatsDClient
import fr.davit.akka.http.metrics.core.{Counter, Dimension, Gauge, Histogram, Timer}

import scala.concurrent.duration.FiniteDuration

object StatsDMetrics {
  def dimensionToTag(dimension: Dimension): String = s"${dimension.key}:${dimension.value}"
}

class StatsDCounter(name: String)(implicit client: StatsDClient) extends Counter {
  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.increment(name, dimensions.map(StatsDMetrics.dimensionToTag): _*)
  }
}

class StatsDGauge(name: String)(implicit client: StatsDClient) extends Gauge {
  override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.increment(name, dimensions.map(StatsDMetrics.dimensionToTag): _*)
  }

  override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.decrement(name, dimensions.map(StatsDMetrics.dimensionToTag): _*)
  }
}

class StatsDTimer(name: String)(implicit client: StatsDClient) extends Timer {
  override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
    client.distribution(name, duration.toMillis, dimensions.map(StatsDMetrics.dimensionToTag): _*)
  }
}

class StatsDHistogram(name: String)(implicit client: StatsDClient) extends Histogram {
  override def update[T](value: T, dimensions: Seq[Dimension] = Seq.empty)(implicit numeric: Numeric[T]): Unit = {
    client.distribution(name, numeric.toDouble(value), dimensions.map(StatsDMetrics.dimensionToTag): _*)
  }
}

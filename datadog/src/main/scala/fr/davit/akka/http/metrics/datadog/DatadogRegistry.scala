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
import fr.davit.akka.http.metrics.core._

import scala.concurrent.duration.FiniteDuration

object DatadogRegistry {

  val AkkaPrefix = "akka.http"

  def name(names: String*): String = s"$AkkaPrefix.${names.mkString("_")}"

  def dimensionToTag(dimension: Dimension): String = s"${dimension.key}:${dimension.value}"

  private implicit class RichStatsDClient(client: StatsDClient) {
    def longCounter(name: String): Counter[Long] = new Counter[Long] {
      override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.increment(name, dimensions.map(dimensionToTag): _*)
      }
    }

    def longGauge(name: String): Gauge[Long] = new Gauge[Long] {
      override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.increment(name, dimensions.map(dimensionToTag): _*)
      }

      override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.decrement(name, dimensions.map(dimensionToTag): _*)
      }
    }

    def timer(name: String): Timer = new Timer {
      override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.distribution(name, duration.toMillis, dimensions.map(dimensionToTag): _*)
      }
    }

    def longHistogram(name: String): Histogram[Long] = new Histogram[Long] {
      override def update(value: Long, dimensions: Seq[Dimension] = Seq.empty): Unit = {
        client.distribution(name, value, dimensions.map(dimensionToTag): _*)
      }
    }
  }

  def apply(client: StatsDClient): DatadogRegistry = new DatadogRegistry(client)
}

/**
  * see [https://docs.datadoghq.com/developers/faq/what-best-practices-are-recommended-for-naming-metrics-and-tags/]
  * @param client
  */
class DatadogRegistry(client: StatsDClient) extends HttpMetricsRegistry {

  import DatadogRegistry._

  override val active: Gauge[Long] = client.longGauge(name("requests", "active"))

  override val requests: Counter[Long] = client.longCounter(name("requests", "count"))

  override val receivedBytes: Histogram[Long] = client.longHistogram(name("requests", "bytes"))

  override val responses: Counter[Long] = client.longCounter(name("responses", "count"))

  override val errors: Counter[Long] = client.longCounter(name("responses", "errors", "count"))

  override val duration: Timer = client.timer(name("responses", "duration"))

  override val sentBytes: Histogram[Long] = client.longHistogram(name("responses", "bytes"))

  override val connected: Gauge[Long] = client.longGauge(name("connections", "active"))

  override val connections: Counter[Long] = client.longCounter(name("connections", "count"))
}

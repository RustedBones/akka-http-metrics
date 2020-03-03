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

import fr.davit.akka.http.metrics.core._
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsSettings

object GraphiteRegistry {
  def apply(client: CarbonClient, settings: HttpMetricsSettings = HttpMetricsSettings.default): GraphiteRegistry = {
    new GraphiteRegistry(settings)(client)
  }
}

class GraphiteRegistry(settings: HttpMetricsSettings)(implicit client: CarbonClient)
    extends HttpMetricsRegistry(settings) {

  override lazy val active: Gauge = new CarbonGauge(settings.namespace, "requests.active")

  override lazy val requests: Counter = new CarbonCounter(settings.namespace,"requests")

  override lazy val receivedBytes: Histogram = new CarbonHistogram(settings.namespace,"requests.bytes")

  override lazy val responses: Counter = new CarbonCounter(settings.namespace,"responses")

  override lazy val errors: Counter = new CarbonCounter(settings.namespace,"responses.errors")

  override lazy val duration: Timer = new CarbonTimer(settings.namespace,"responses.duration")

  override lazy val sentBytes: Histogram = new CarbonHistogram(settings.namespace,"responses.bytes")

  override lazy val connected: Gauge = new CarbonGauge(settings.namespace,"connections.active")

  override lazy val connections: Counter = new CarbonCounter(settings.namespace,"connections")
}

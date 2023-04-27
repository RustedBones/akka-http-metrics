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

package fr.davit.pekko.http.metrics.graphite

import fr.davit.pekko.http.metrics.core.{HttpMetricsSettings, _}

object GraphiteRegistry {

  def apply(client: CarbonClient, settings: HttpMetricsSettings = GraphiteSettings.default): GraphiteRegistry = {
    new GraphiteRegistry(settings)(client)
  }
}

class GraphiteRegistry(settings: HttpMetricsSettings)(implicit client: CarbonClient)
    extends HttpMetricsRegistry(settings) {

  lazy val requests: Counter         = new CarbonCounter(settings.namespace, settings.metricsNames.requests)
  lazy val requestsActive: Gauge     = new CarbonGauge(settings.namespace, settings.metricsNames.requestsActive)
  lazy val requestsFailures: Counter = new CarbonCounter(settings.namespace, settings.metricsNames.requestsFailures)
  lazy val requestsSize: Histogram   = new CarbonHistogram(settings.namespace, settings.metricsNames.requestsSize)
  lazy val responses: Counter        = new CarbonCounter(settings.namespace, settings.metricsNames.responses)
  lazy val responsesErrors: Counter  = new CarbonCounter(settings.namespace, settings.metricsNames.responsesErrors)
  lazy val responsesDuration: Timer  = new CarbonTimer(settings.namespace, settings.metricsNames.responsesDuration)
  lazy val responsesSize: Histogram  = new CarbonHistogram(settings.namespace, settings.metricsNames.responsesSize)
  lazy val connections: Counter      = new CarbonCounter(settings.namespace, settings.metricsNames.connections)
  lazy val connectionsActive: Gauge  = new CarbonGauge(settings.namespace, settings.metricsNames.connectionsActive)
}

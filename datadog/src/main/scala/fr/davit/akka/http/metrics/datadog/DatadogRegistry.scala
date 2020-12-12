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
import fr.davit.akka.http.metrics.core.{HttpMetricsSettings, _}

object DatadogRegistry {

  def apply(client: StatsDClient, settings: HttpMetricsSettings = DatadogSettings.default): DatadogRegistry = {
    new DatadogRegistry(settings)(client)
  }
}

/**
  * see [https://docs.datadoghq.com/developers/faq/what-best-practices-are-recommended-for-naming-metrics-and-tags/]
  * @param client
  */
class DatadogRegistry(settings: HttpMetricsSettings)(implicit client: StatsDClient)
    extends HttpMetricsRegistry(settings) {

  lazy val requests: Counter         = new StatsDCounter(settings.namespace, settings.metricsNames.requests)
  lazy val requestsActive: Gauge     = new StatsDGauge(settings.namespace, settings.metricsNames.requestsActive)
  lazy val requestsFailures: Counter = new StatsDCounter(settings.namespace, settings.metricsNames.requestsFailures)
  lazy val requestsSize: Histogram   = new StatsDHistogram(settings.namespace, settings.metricsNames.requestsSize)
  lazy val responses: Counter        = new StatsDCounter(settings.namespace, settings.metricsNames.responses)
  lazy val responsesErrors: Counter  = new StatsDCounter(settings.namespace, settings.metricsNames.responsesErrors)
  lazy val responsesDuration: Timer  = new StatsDTimer(settings.namespace, settings.metricsNames.responsesDuration)
  lazy val responsesSize: Histogram  = new StatsDHistogram(settings.namespace, settings.metricsNames.responsesSize)
  lazy val connections: Counter      = new StatsDCounter(settings.namespace, settings.metricsNames.connections)
  lazy val connectionsActive: Gauge  = new StatsDGauge(settings.namespace, settings.metricsNames.connectionsActive)
}

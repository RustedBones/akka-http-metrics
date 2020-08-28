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

import fr.davit.akka.http.metrics.core.{HttpMetricsSettings, _}
import io.dropwizard.metrics5.MetricRegistry

object DropwizardRegistry {

  def apply(
      registry: MetricRegistry = new MetricRegistry(),
      settings: HttpMetricsSettings = DropwizardSettings.default
  ): DropwizardRegistry = {
    new DropwizardRegistry(settings)(registry)
  }
}

class DropwizardRegistry(settings: HttpMetricsSettings)(implicit val underlying: MetricRegistry)
    extends HttpMetricsRegistry(settings) {

  lazy val requests: Counter        = new DropwizardCounter(settings.namespace, settings.metricsNames.requests)
  lazy val requestsActive: Gauge    = new DropwizardGauge(settings.namespace, settings.metricsNames.requestsActive)
  lazy val requestsSize: Histogram  = new DropwizardHistogram(settings.namespace, settings.metricsNames.requestsSize)
  lazy val responses: Counter       = new DropwizardCounter(settings.namespace, settings.metricsNames.responses)
  lazy val responsesErrors: Counter = new DropwizardCounter(settings.namespace, settings.metricsNames.responsesErrors)
  lazy val responsesDuration: Timer = new DropwizardTimer(settings.namespace, settings.metricsNames.responsesDuration)
  lazy val responsesSize: Histogram  = new DropwizardHistogram(settings.namespace, settings.metricsNames.responsesSize)
  lazy val connections: Counter     = new DropwizardCounter(settings.namespace, settings.metricsNames.connections)
  lazy val connectionsActive: Gauge = new DropwizardGauge(settings.namespace, settings.metricsNames.connectionsActive)
}

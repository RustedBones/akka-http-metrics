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

  lazy val active: Gauge            = new DropwizardGauge(settings.namespace, settings.metricsNames.activeRequests)
  lazy val requests: Counter        = new DropwizardCounter(settings.namespace, settings.metricsNames.requests)
  lazy val receivedBytes: Histogram = new DropwizardHistogram(settings.namespace, settings.metricsNames.requestSizes)
  lazy val responses: Counter       = new DropwizardCounter(settings.namespace, settings.metricsNames.responses)
  lazy val errors: Counter          = new DropwizardCounter(settings.namespace, settings.metricsNames.errors)
  lazy val duration: Timer          = new DropwizardTimer(settings.namespace, settings.metricsNames.durations)
  lazy val sentBytes: Histogram     = new DropwizardHistogram(settings.namespace, settings.metricsNames.responseSizes)
  lazy val connected: Gauge         = new DropwizardGauge(settings.namespace, settings.metricsNames.activeConnections)
  lazy val connections: Counter     = new DropwizardCounter(settings.namespace, settings.metricsNames.connections)
}

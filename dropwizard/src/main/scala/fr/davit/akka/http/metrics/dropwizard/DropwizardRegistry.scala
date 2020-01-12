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

import fr.davit.akka.http.metrics.core._
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsSettings
import io.dropwizard.metrics5.MetricRegistry

object DropwizardRegistry {

  def apply(
      registry: MetricRegistry = new MetricRegistry(),
      settings: HttpMetricsSettings = HttpMetricsSettings.default
  ): DropwizardRegistry = {
    new DropwizardRegistry(settings)(registry)
  }
}

class DropwizardRegistry(settings: HttpMetricsSettings)(implicit val underlying: MetricRegistry)
    extends HttpMetricsRegistry(settings) {

  override lazy val active: Gauge = new DropwizardGauge("akka.http.requests.active")

  override lazy val requests: Counter = new DropwizardCounter("akka.http.requests")

  override lazy val receivedBytes: Histogram = new DropwizardHistogram("akka.http.requests.bytes")

  override lazy val responses: Counter = new DropwizardCounter("akka.http.responses")

  override lazy val errors: Counter = new DropwizardCounter("akka.http.responses.errors")

  override lazy val duration: Timer = new DropwizardTimer("akka.http.responses.duration")

  override lazy val sentBytes: Histogram = new DropwizardHistogram("akka.http.responses.bytes")

  override lazy val connected: Gauge = new DropwizardGauge("akka.http.connections.active")

  override lazy val connections: Counter = new DropwizardCounter("akka.http.connections")
}

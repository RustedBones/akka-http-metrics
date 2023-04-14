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

import org.apache.pekko.http.scaladsl.model.StatusCodes
import fr.davit.akka.http.metrics.core.HttpMetricsNames.HttpMetricsNamesImpl
import fr.davit.akka.http.metrics.core.{HttpMetricsNames, HttpMetricsSettings}
import fr.davit.akka.http.metrics.core.HttpMetricsSettings.HttpMetricsSettingsImpl

object DatadogMetricsNames {

  val default: HttpMetricsNames = HttpMetricsNamesImpl(
    requests = "requests_count",
    requestsActive = "requests_active",
    requestsFailures = "requests_failures_count",
    requestsSize = "requests_bytes",
    responses = "responses_count",
    responsesErrors = "responses_errors_count",
    responsesDuration = "responses_duration",
    responsesSize = "responses_bytes",
    connections = "connections_count",
    connectionsActive = "connections_active"
  )

}

object DatadogSettings {

  val default: HttpMetricsSettings = HttpMetricsSettingsImpl(
    "akka.http",
    DatadogMetricsNames.default,
    _.status.isInstanceOf[StatusCodes.ServerError],
    includeMethodDimension = false,
    includePathDimension = false,
    includeStatusDimension = false
  )

}

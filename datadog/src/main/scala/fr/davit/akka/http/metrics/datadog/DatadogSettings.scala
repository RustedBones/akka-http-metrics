package fr.davit.akka.http.metrics.datadog

import akka.http.scaladsl.model.StatusCodes
import fr.davit.akka.http.metrics.core.HttpMetricsNames.HttpMetricsNamesImpl
import fr.davit.akka.http.metrics.core.{HttpMetricsNames, HttpMetricsSettings}
import fr.davit.akka.http.metrics.core.HttpMetricsSettings.HttpMetricsSettingsImpl

object DatadogMetricsNames {

  val default: HttpMetricsNames = HttpMetricsNamesImpl(
    requests = "requests_count",
    requestsActive = "requests_active",
    requestsSize = "requests_bytes",
    responses = "responses_count",
    responsesErrors = "responses_errors_count",
    responsesDuration = "responses_duration",
    responsesSize = "responses_bytes",
    connections = "connections_active",
    connectionsActive = "connections_count"
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

package fr.davit.akka.http.metrics.datadog

import akka.http.scaladsl.model.StatusCodes
import fr.davit.akka.http.metrics.core.HttpMetricsNames.HttpMetricsNamesImpl
import fr.davit.akka.http.metrics.core.{HttpMetricsNames, HttpMetricsSettings}
import fr.davit.akka.http.metrics.core.HttpMetricsSettings.HttpMetricsSettingsImpl

object DatadogMetricsNames {

  val default: HttpMetricsNames = HttpMetricsNamesImpl(
    requests = "requests_count",
    activeRequests = "requests_active",
    requestSizes = "requests_bytes",
    responses = "responses_count",
    errors = "responses_errors_count",
    durations = "responses_duration",
    responseSizes = "responses_bytes",
    connections = "connections_active",
    activeConnections = "connections_count"
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

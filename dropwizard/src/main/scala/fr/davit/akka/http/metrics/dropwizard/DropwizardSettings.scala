package fr.davit.akka.http.metrics.dropwizard

import akka.http.scaladsl.model.StatusCodes
import fr.davit.akka.http.metrics.core.HttpMetricsNames.HttpMetricsNamesImpl
import fr.davit.akka.http.metrics.core.{HttpMetricsNames, HttpMetricsSettings}
import fr.davit.akka.http.metrics.core.HttpMetricsSettings.HttpMetricsSettingsImpl

object DropwizardMetricsNames {

  val default: HttpMetricsNames = HttpMetricsNamesImpl(
    requests = "requests",
    activeRequests = "requests.active",
    requestSizes = "requests.bytes",
    responses = "responses",
    errors = "responses.errors",
    durations = "responses.duration",
    responseSizes = "responses.bytes",
    connections = "connections",
    activeConnections = "connections.active"
  )

}

object DropwizardSettings {

  val default: HttpMetricsSettings = HttpMetricsSettingsImpl(
    "akka.http",
    DropwizardMetricsNames.default,
    _.status.isInstanceOf[StatusCodes.ServerError],
    includeMethodDimension = false,
    includePathDimension = false,
    includeStatusDimension = false
  )

}

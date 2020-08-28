package fr.davit.akka.http.metrics.dropwizard

import akka.http.scaladsl.model.StatusCodes
import fr.davit.akka.http.metrics.core.HttpMetricsNames.HttpMetricsNamesImpl
import fr.davit.akka.http.metrics.core.{HttpMetricsNames, HttpMetricsSettings}
import fr.davit.akka.http.metrics.core.HttpMetricsSettings.HttpMetricsSettingsImpl

object DropwizardMetricsNames {

  val default: HttpMetricsNames = HttpMetricsNamesImpl(
    requests = "requests",
    requestsActive = "requests.active",
    requestsSize = "requests.bytes",
    responses = "responses",
    responsesErrors = "responses.errors",
    responsesDuration = "responses.duration",
    responsesSize = "responses.bytes",
    connections = "connections",
    connectionsActive = "connections.active"
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

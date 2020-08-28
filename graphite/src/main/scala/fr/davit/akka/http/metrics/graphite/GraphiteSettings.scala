package fr.davit.akka.http.metrics.graphite

import akka.http.scaladsl.model.StatusCodes
import fr.davit.akka.http.metrics.core.HttpMetricsNames.HttpMetricsNamesImpl
import fr.davit.akka.http.metrics.core.HttpMetricsSettings.HttpMetricsSettingsImpl
import fr.davit.akka.http.metrics.core.{HttpMetricsNames, HttpMetricsSettings}

object GraphiteMetricsNames {

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

object GraphiteSettings {

  val default: HttpMetricsSettings = HttpMetricsSettingsImpl(
    "akka.http",
    GraphiteMetricsNames.default,
    _.status.isInstanceOf[StatusCodes.ServerError],
    includeMethodDimension = false,
    includePathDimension = false,
    includeStatusDimension = false
  )

}

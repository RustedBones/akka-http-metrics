package fr.davit.akka.http.metrics.core.scaladsl.server

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

trait HttpMetricsSettings {

  /**
    * Function that defines if the http response should be
    * counted as an error
    */
  def defineError: HttpResponse => Boolean

  def withDefineError(fn: HttpResponse => Boolean): HttpMetricsSettings
}

object HttpMetricsSettings {

  val default: HttpMetricsSettings = apply(_.status.isInstanceOf[StatusCodes.ServerError])

  def apply(defineError: HttpResponse => Boolean): HttpMetricsSettings = HttpMetricsSettingsImpl(defineError)

  private case class HttpMetricsSettingsImpl(defineError: HttpResponse => Boolean) extends HttpMetricsSettings {
    override def withDefineError(fn: HttpResponse => Boolean): HttpMetricsSettings = copy(defineError = fn)
  }
}

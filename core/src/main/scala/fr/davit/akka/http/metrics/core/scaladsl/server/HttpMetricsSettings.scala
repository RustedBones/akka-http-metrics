package fr.davit.akka.http.metrics.core.scaladsl.server

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

trait HttpMetricsSettings {

  /**
    * Function that defines if the http response should be
    * counted as an error
    */
  def defineError: HttpResponse => Boolean

  /**
    * Include the status group dimension on metrics
    */
  def includeStatusDimension: Boolean

  /**
    * Include the path dimension on metrics
    */
  def includePathDimension: Boolean

  def withDefineError(fn: HttpResponse => Boolean): HttpMetricsSettings
  def withIncludeStatusDimension(include: Boolean): HttpMetricsSettings
  def withIncludePathDimension(include: Boolean): HttpMetricsSettings
}

object HttpMetricsSettings {

  val default: HttpMetricsSettings = apply(
    _.status.isInstanceOf[StatusCodes.ServerError],
    includeStatusDimension = false,
    includePathDimension = false
  )

  def apply(
      defineError: HttpResponse => Boolean,
      includeStatusDimension: Boolean,
      includePathDimension: Boolean): HttpMetricsSettings = HttpMetricsSettingsImpl(
    defineError,
    includeStatusDimension,
    includePathDimension
  )

  private case class HttpMetricsSettingsImpl(
      defineError: HttpResponse => Boolean,
      includeStatusDimension: Boolean,
      includePathDimension: Boolean
  ) extends HttpMetricsSettings {

    override def withDefineError(fn: HttpResponse => Boolean): HttpMetricsSettings =
      copy(defineError = fn)
    override def withIncludeStatusDimension(include: Boolean): HttpMetricsSettings =
      copy(includeStatusDimension = include)
    override def withIncludePathDimension(include: Boolean): HttpMetricsSettings =
      copy(includePathDimension = include)

  }
}

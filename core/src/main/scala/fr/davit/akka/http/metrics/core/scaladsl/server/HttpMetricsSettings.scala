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
      includePathDimension: Boolean
  ): HttpMetricsSettings = HttpMetricsSettingsImpl(
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

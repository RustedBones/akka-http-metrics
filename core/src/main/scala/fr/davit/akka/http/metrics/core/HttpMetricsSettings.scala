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

package fr.davit.akka.http.metrics.core

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

trait HttpMetricsSettings {

  /**
    * Metrics namespace
    */
  def namespace: String

  /**
    * Function that defines if the http response should be
    * counted as an error
    */
  def defineError: HttpResponse => Boolean

  /**
    * Include the method dimension on metrics
    */
  def includeMethodDimension: Boolean

  /**
    * Include the path dimension on metrics
    */
  def includePathDimension: Boolean

  /**
    * Include the status group dimension on metrics
    */
  def includeStatusDimension: Boolean

  def withNamespace(namespace: String): HttpMetricsSettings
  def withDefineError(fn: HttpResponse => Boolean): HttpMetricsSettings
  def withIncludeMethodDimension(include: Boolean): HttpMetricsSettings
  def withIncludePathDimension(include: Boolean): HttpMetricsSettings
  def withIncludeStatusDimension(include: Boolean): HttpMetricsSettings
}

object HttpMetricsSettings {

  val default: HttpMetricsSettings = apply(
    "akka.http",
    _.status.isInstanceOf[StatusCodes.ServerError],
    includeMethodDimension = false,
    includePathDimension = false,
    includeStatusDimension = false
  )

  def apply(
      namespace: String,
      defineError: HttpResponse => Boolean,
      includeMethodDimension: Boolean,
      includePathDimension: Boolean,
      includeStatusDimension: Boolean
  ): HttpMetricsSettings = HttpMetricsSettingsImpl(
    namespace,
    defineError,
    includeMethodDimension,
    includePathDimension,
    includeStatusDimension
  )

  private case class HttpMetricsSettingsImpl(
      namespace: String,
      defineError: HttpResponse => Boolean,
      includeMethodDimension: Boolean,
      includePathDimension: Boolean,
      includeStatusDimension: Boolean
  ) extends HttpMetricsSettings {

    override def withNamespace(namespace: String): HttpMetricsSettings =
      copy(namespace = namespace)
    override def withDefineError(fn: HttpResponse => Boolean): HttpMetricsSettings =
      copy(defineError = fn)
    override def withIncludeMethodDimension(include: Boolean): HttpMetricsSettings =
      copy(includeMethodDimension = include)
    override def withIncludePathDimension(include: Boolean): HttpMetricsSettings =
      copy(includePathDimension = include)
    override def withIncludeStatusDimension(include: Boolean): HttpMetricsSettings =
      copy(includeStatusDimension = include)

  }
}

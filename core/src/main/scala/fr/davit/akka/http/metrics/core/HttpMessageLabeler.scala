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

import akka.http.scaladsl.model._
import fr.davit.akka.http.metrics.core.HttpMessageLabeler.Unlabelled

object HttpMessageLabeler {
  val Unlabelled = "unlabelled"
}

/** Create the labels for a given HTTP dimension This is not intended to be used directly. Please extend
  * [[HttpResponseLabeler]] or [[HttpResponseLabeler]]
  *
  * @tparam T
  *   type of HTTP message to label ([[HttpResponse]] or [[HttpResponse]])
  */
sealed trait HttpMessageLabeler[T <: HttpMessage] {

  /** The dimension name */
  def name: String

  /** The label for the message */
  def label(message: T): String

  /** the metric [[Dimension]] for the message */
  def dimension(message: T): Dimension = Dimension(name, label(message))
}

trait HttpRequestLabeler extends HttpMessageLabeler[HttpRequest]
trait HttpResponseLabeler extends HttpMessageLabeler[HttpResponse]

object MethodLabeler extends HttpRequestLabeler {
  override def name                                = "method"
  override def label(request: HttpRequest): String = request.method.value
}

object StatusGroupLabeler extends HttpResponseLabeler {
  override def name = "status"
  override def label(response: HttpResponse): String = response.status match {
    case _: StatusCodes.Success     => "2xx"
    case _: StatusCodes.Redirection => "3xx"
    case _: StatusCodes.ClientError => "4xx"
    case _: StatusCodes.ServerError => "5xx"
    case _                          => "other"
  }
}

trait AttributeLabeler extends HttpResponseLabeler {
  lazy val key: AttributeKey[String]                 = AttributeKey(s"metrics-$name-label")
  override def label(response: HttpResponse): String = response.attribute(key).getOrElse(Unlabelled)
}

object PathLabeler extends AttributeLabeler {
  override def name: String = "path"
}

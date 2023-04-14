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

import org.apache.pekko.http.scaladsl.model._
import fr.davit.akka.http.metrics.core.HttpMessageLabeler.Unlabelled

object HttpMessageLabeler {
  val Unlabelled = "unlabelled"
}

/** Create the labels for a given HTTP dimension See [[HttpResponseLabeler]] or [[HttpResponseLabeler]]
  */
sealed trait HttpMessageLabeler {

  /** The dimension name */
  def name: String
}

trait HttpRequestLabeler extends HttpMessageLabeler {

  /** The label for the request */
  def label(request: HttpRequest): String

  /** the metric [[Dimension]] for the request */
  def dimension(request: HttpRequest): Dimension = Dimension(name, label(request))
}
trait HttpResponseLabeler extends HttpMessageLabeler {

  /** The label for the message */
  def label(response: HttpResponse): String

  /** the metric [[Dimension]] for the response */
  def dimension(response: HttpResponse): Dimension = Dimension(name, label(response))
}

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

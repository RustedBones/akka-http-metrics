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

import akka.Done
import akka.http.scaladsl.model._
import akka.stream.Materializer
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{MethodDimension, PathDimension, StatusGroupDimension}
import fr.davit.akka.http.metrics.core.scaladsl.model.PathLabelHeader

import scala.concurrent.duration.Deadline
import scala.concurrent.{ExecutionContext, Future}

object HttpMetricsRegistry {

  object MethodDimension {
    val Key: String = "method"
  }

  final case class MethodDimension(method: HttpMethod) extends Dimension {
    override def key: String   = MethodDimension.Key
    override def value: String = method.value
  }

  object PathDimension {
    val Key: String = "path"
  }

  final case class PathDimension(value: String) extends Dimension {
    override def key = PathDimension.Key
  }

  object StatusGroupDimension {
    val Key: String = "status"
  }

  final case class StatusGroupDimension(code: StatusCode) extends Dimension {
    override def key: String = StatusGroupDimension.Key
    override def value: String = code match {
      case _: StatusCodes.Success     => "2xx"
      case _: StatusCodes.Redirection => "3xx"
      case _: StatusCodes.ClientError => "4xx"
      case _: StatusCodes.ServerError => "5xx"
      case _                          => "other"
    }
  }
}

abstract class HttpMetricsRegistry(settings: HttpMetricsSettings) extends HttpMetricsHandler {

  //--------------------------------------------------------------------------------------------------------------------
  // requests
  //--------------------------------------------------------------------------------------------------------------------
  def requests: Counter

  def requestsActive: Gauge

  def requestsSize: Histogram

  @deprecated("Use requestsActive", "1.2.0")
  def active: Gauge = requestsActive

  @deprecated("Use requestsSize", "1.2.0")
  def receivedBytes: Histogram = requestsSize

  //--------------------------------------------------------------------------------------------------------------------
  // responses
  //--------------------------------------------------------------------------------------------------------------------
  def responses: Counter

  def responsesErrors: Counter

  def responsesDuration: Timer

  def responsesSize: Histogram

  @deprecated("Use responsesErrors", "1.2.0")
  def errors: Counter = responsesErrors

  @deprecated("Use responsesDuration", "1.2.0")
  def duration: Timer = responsesDuration

  @deprecated("Use responsesSize", "1.2.0")
  def sentBytes: Histogram = responsesSize

  //--------------------------------------------------------------------------------------------------------------------
  // Connections
  //--------------------------------------------------------------------------------------------------------------------
  def connections: Counter

  def connectionsActive: Gauge

  @deprecated("Use connectionsActive", "1.2.0")
  def connected: Gauge = connectionsActive

  private def pathLabel(response: HttpResponse): String = {
    response.header[PathLabelHeader].getOrElse(PathLabelHeader.UnLabelled).value
  }

  // Since Content-Length header can't be relied on, see [[HttpEntity.contentLengthOption]]:
  // In many cases it's dangerous to rely on the (non-)existence of a content-length.
  // Compute the entity size from the data itself
  private def entitySize(entity: HttpEntity)(implicit mat: Materializer): Future[Long] =
    entity.dataBytes.map(_.length).runFold(0L)(_ + _)

  override def onRequest(request: HttpRequest, response: Future[HttpResponse])(
      implicit fm: Materializer
  ): Unit = {
    implicit val ec: ExecutionContext = fm.executionContext
    val serverDimensions              = settings.serverDimensions

    requestsActive.inc(serverDimensions)
    requests.inc(serverDimensions)
    entitySize(request.entity).foreach(requestsSize.update(_, serverDimensions))
    val start = Deadline.now

    response.foreach { r =>
      // compute dimensions
      // format: off
      val methodDim = if (settings.includeMethodDimension) Some(MethodDimension(request.method)) else None
      val pathDim = if (settings.includePathDimension) Some(PathDimension(pathLabel(r))) else None
      val statusGroupDim = if (settings.includeStatusDimension) Some(StatusGroupDimension(r.status)) else None

      val dimensions = (methodDim ++ pathDim ++ statusGroupDim).toSeq ++ serverDimensions
      // format: on

      requestsActive.dec(serverDimensions)
      responses.inc(dimensions)
      responsesDuration.observe(Deadline.now - start, dimensions)
      if (settings.defineError(r)) {
        responsesErrors.inc(dimensions)
      }
      entitySize(r.entity).foreach(responsesSize.update(_, dimensions))
    }
  }

  override def onConnection(completion: Future[Done])(implicit fm: Materializer): Unit = {
    val serverDimensions = settings.serverDimensions
    connections.inc(serverDimensions)
    connectionsActive.inc(serverDimensions)
    completion.onComplete(_ => connectionsActive.dec(serverDimensions))(fm.executionContext)
  }
}

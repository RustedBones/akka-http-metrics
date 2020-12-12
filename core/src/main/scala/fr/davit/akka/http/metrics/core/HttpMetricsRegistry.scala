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
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.ByteString

import scala.concurrent.duration.Deadline

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

  import HttpMetricsRegistry._

  //--------------------------------------------------------------------------------------------------------------------
  // requests
  //--------------------------------------------------------------------------------------------------------------------
  def requests: Counter

  def requestsActive: Gauge

  def requestsFailures: Counter

  def requestsSize: Histogram

  //--------------------------------------------------------------------------------------------------------------------
  // responses
  //--------------------------------------------------------------------------------------------------------------------
  def responses: Counter

  def responsesErrors: Counter

  def responsesDuration: Timer

  def responsesSize: Histogram

  //--------------------------------------------------------------------------------------------------------------------
  // Connections
  //--------------------------------------------------------------------------------------------------------------------
  def connections: Counter

  def connectionsActive: Gauge

  private def methodDimension(request: HttpRequest): Option[MethodDimension] = {
    if (settings.includeMethodDimension) Some(MethodDimension(request.method)) else None
  }

  private def pathDimension(response: HttpResponse): Option[PathDimension] = {
    val pathLabel = response.attribute(HttpMetrics.PathLabel).getOrElse("unlabelled")
    if (settings.includePathDimension) Some(PathDimension(pathLabel)) else None
  }

  private def statusGroupDimension(response: HttpResponse): Option[StatusGroupDimension] = {
    if (settings.includeStatusDimension) Some(StatusGroupDimension(response.status)) else None
  }

  private def onData(handler: Long => Unit): Flow[ByteString, ByteString, Any] = {
    val collectSizeSink = Flow[ByteString]
      .map(_.length)
      .fold(0L)(_ + _)
      .to(Sink.foreach(handler))
    Flow[ByteString].alsoTo(collectSizeSink)
  }

  // Since Content-Length header can't be relied on, see [[HttpEntity.contentLengthOption]]:
  // In many cases it's dangerous to rely on the (non-)existence of a content-length.
  // Compute the entity size from the data itself
  private def requestBytesTransformer(dimensions: Seq[Dimension]): Flow[ByteString, ByteString, Any] =
    onData(requestsSize.update(_, dimensions))

  // Same for response, and also observe duration only when all bytes are sent
  private def responseBytesTransformer(
      request: HttpRequest,
      dimension: Seq[Dimension]
  ): Flow[ByteString, ByteString, Any] =
    onData { size =>
      val start    = request.attribute(HttpMetrics.TraceTimestamp).get
      val duration = Deadline.now - start
      responsesSize.update(size, dimension)
      responsesDuration.observe(duration, dimension)
    }

  override def onRequest(request: HttpRequest): HttpRequest = {
    val dimensions = settings.serverDimensions ++ methodDimension(request)
    requestsActive.inc(dimensions)
    requests.inc(dimensions)
    request.transformEntityDataBytes(requestBytesTransformer(dimensions))
  }

  override def onResponse(request: HttpRequest, response: HttpResponse): HttpResponse = {
    val requestDimensions  = settings.serverDimensions ++ methodDimension(request)
    val responseDimensions = requestDimensions ++ pathDimension(response) ++ statusGroupDimension(response)

    requestsActive.dec(requestDimensions)
    responses.inc(responseDimensions)
    if (settings.defineError(response)) responsesErrors.inc(responseDimensions)
    response.transformEntityDataBytes(responseBytesTransformer(request, responseDimensions))
  }

  override def onFailure(request: HttpRequest, cause: Throwable): Throwable = {
    val dimensions = settings.serverDimensions ++ methodDimension(request)
    requestsActive.dec(dimensions)
    requestsFailures.inc(dimensions)
    cause
  }

  override def onConnection(): Unit = {
    connections.inc(settings.serverDimensions)
    connectionsActive.inc(settings.serverDimensions)
  }

  override def onDisconnection(): Unit = {
    connectionsActive.dec(settings.serverDimensions)
  }
}

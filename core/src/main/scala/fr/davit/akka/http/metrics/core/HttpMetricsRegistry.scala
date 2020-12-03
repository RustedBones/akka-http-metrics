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

import java.util.UUID
import scala.collection.concurrent
import scala.collection.concurrent.TrieMap
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
    response.attribute(HttpMetrics.PathLabel).getOrElse("unlabelled")
  }

  private val pendingRequests: concurrent.Map[UUID, (HttpRequest, Deadline)] = TrieMap.empty

  // Since Content-Length header can't be relied on, see [[HttpEntity.contentLengthOption]]:
  // In many cases it's dangerous to rely on the (non-)existence of a content-length.
  // Compute the entity size from the data itself
  private def onData(handler: Long => Unit): Flow[ByteString, ByteString, Any] = {
    val collectSizeSink = Flow[ByteString]
      .map(_.length)
      .fold(0L)(_ + _)
      .to(Sink.foreach(handler))
    Flow[ByteString].wireTap(collectSizeSink)
  }

  private val requestBytesTransformer  = onData(requestsSize.update(_, settings.serverDimensions))
  private val responseBytesTransformer = onData(responsesSize.update(_, settings.serverDimensions))

  override def onRequest(request: HttpRequest): HttpRequest = {
    val id = request.attribute(HttpMetrics.TracingId).get
    pendingRequests.put(id, (request, Deadline.now))
    requestsActive.inc(settings.serverDimensions)
    requests.inc(settings.serverDimensions)
    request.transformEntityDataBytes(requestBytesTransformer)
  }

  override def onResponse(response: HttpResponse): HttpResponse = {
    val id               = response.attribute(HttpMetrics.TracingId).get
    val (request, start) = pendingRequests(id)
    pendingRequests.remove(id)
    val pathDim        = if (settings.includePathDimension) Some(PathDimension(pathLabel(response))) else None
    val statusGroupDim = if (settings.includeStatusDimension) Some(StatusGroupDimension(response.status)) else None
    val methodDim      = if (settings.includeMethodDimension) Some(MethodDimension(request.method)) else None
    val dimensions     = (methodDim ++ pathDim ++ statusGroupDim).toSeq ++ settings.serverDimensions

    requestsActive.dec(settings.serverDimensions)
    responses.inc(dimensions)
    responsesDuration.observe(Deadline.now - start, dimensions)
    if (settings.defineError(response)) {
      responsesErrors.inc(dimensions)
    }
    response.transformEntityDataBytes(responseBytesTransformer)
  }

  override def onConnection(): Unit = {
    connections.inc(settings.serverDimensions)
    connectionsActive.inc(settings.serverDimensions)
  }

  override def onDisconnection(): Unit = {
    connectionsActive.dec(settings.serverDimensions)
  }
}

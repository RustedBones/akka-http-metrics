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
    override def key: String = PathDimension.Key
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
  import HttpMetrics._

  // --------------------------------------------------------------------------------------------------------------------
  // requests
  // --------------------------------------------------------------------------------------------------------------------
  def requests: Counter

  def requestsActive: Gauge

  def requestsSize: Histogram

  // --------------------------------------------------------------------------------------------------------------------
  // responses
  // --------------------------------------------------------------------------------------------------------------------
  def responses: Counter

  def responsesDuration: Timer

  def responsesSize: Histogram

  // --------------------------------------------------------------------------------------------------------------------
  // Connections
  // --------------------------------------------------------------------------------------------------------------------
  def connections: Counter

  def connectionsActive: Gauge

  private def methodDimension(message: HttpMessage): Option[MethodDimension] = {
    val method = message match {
      case request: HttpRequest   => request.method
      case response: HttpResponse => response.attribute(RequestMethod.Key).map(_.method).get
    }
    if (settings.includeMethodDimension) Some(MethodDimension(method)) else None
  }

  private def pathDimension(response: HttpResponse): Option[PathDimension] = {
    val attribute = response.attribute(RequestPath.Key)
    val label     = attribute.map(_.label).getOrElse("unlabelled")
    if (settings.includePathDimension) Some(PathDimension(label)) else None
  }

  private def statusGroupDimension(response: HttpResponse): Option[StatusGroupDimension] = {
    if (settings.includeStatusDimension) Some(StatusGroupDimension(response.status)) else None
  }

  override def onRequest(request: HttpRequest): HttpRequest = {
    val timestamp  = Deadline.now
    val dimensions = settings.serverDimensions ++ methodDimension(request)
    requestsActive.inc(dimensions)
    requests.inc(dimensions)

    val attributes = request.attributes ++ Map(
      RequestMethod.Key    -> RequestMethod(request.method),
      RequestTimestamp.Key -> RequestTimestamp(timestamp)
    )
    val entity = request.entity match {
      case data: HttpEntity.Strict =>
        requestsSize.update(data.contentLength, dimensions)
        data
      case data: HttpEntity.Default =>
        requestsSize.update(data.contentLength, dimensions)
        data
      case data: HttpEntity.Chunked =>
        val collectSizeSink = Flow[ByteString]
          .map(_.length)
          .fold(0L)(_ + _)
          .to(Sink.foreach(size => requestsSize.update(size, dimensions)))
        data.transformDataBytes(Flow[ByteString].alsoTo(collectSizeSink))
    }

    // modify the request
    request
      .withAttributes(attributes)
      .withEntity(entity)
  }

  override def onResponse(response: HttpResponse): HttpResponse = {
    val start = response.attribute(RequestTimestamp.Key).get.value

    val requestDimensions  = settings.serverDimensions ++ methodDimension(response)
    val responseDimensions = requestDimensions ++ pathDimension(response) ++ statusGroupDimension(response)

    requestsActive.dec(requestDimensions)
    responses.inc(responseDimensions)
    response.entity match {
      case data: HttpEntity.Strict =>
        responsesSize.update(data.contentLength, responseDimensions)
        responsesDuration.observe(Deadline.now - start, responseDimensions)
        response
      case data: HttpEntity.Default =>
        responsesSize.update(data.contentLength, responseDimensions)
        responsesDuration.observe(Deadline.now - start, responseDimensions)
        response
      case _: HttpEntity.Chunked | _: HttpEntity.CloseDelimited =>
        val collectSizeSink = Flow[ByteString]
          .map(_.length)
          .fold(0L)(_ + _)
          .to(Sink.foreach { size =>
            responsesSize.update(size, responseDimensions)
            responsesDuration.observe(Deadline.now - start, responseDimensions)
          })
        response.transformEntityDataBytes(Flow[ByteString].alsoTo(collectSizeSink))
    }
  }

  override def onConnection(): Unit = {
    connections.inc(settings.serverDimensions)
    connectionsActive.inc(settings.serverDimensions)
  }

  override def onDisconnection(): Unit = {
    connectionsActive.dec(settings.serverDimensions)
  }
}

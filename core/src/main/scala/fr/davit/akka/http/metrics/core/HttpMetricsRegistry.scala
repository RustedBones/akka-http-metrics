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
  private[metrics] val TraceTimestampKey: AttributeKey[Deadline] = AttributeKey("trace-time")
}

abstract class HttpMetricsRegistry(settings: HttpMetricsSettings) extends HttpMetricsHandler {

  import HttpMetricsRegistry._

  private val requestLabelers: Seq[HttpRequestLabeler] = {
    val builder = Seq.newBuilder[HttpRequestLabeler]
    if (settings.includeMethodDimension) builder += MethodLabeler
    builder ++= settings.customDimensions.collect { case l: HttpRequestLabeler => l }
    builder.result()
  }

  private val responseLabelers: Seq[HttpResponseLabeler] = {
    val builder = Seq.newBuilder[HttpResponseLabeler]
    if (settings.includeStatusDimension) builder += StatusGroupLabeler
    if (settings.includePathDimension) builder += PathLabeler
    builder ++= settings.customDimensions.collect { case l: HttpResponseLabeler => l }
    builder.result()
  }

  protected def requestDimensions(request: HttpRequest): Seq[Dimension] = {
    requestLabelers.map(_.dimension(request))
  }

  protected def responseDimensions(response: HttpResponse): Seq[Dimension] = {
    responseLabelers.map(_.dimension(response))
  }

  override def onRequest(request: HttpRequest): HttpRequest = {
    val start = Deadline.now
    val dims  = settings.serverDimensions ++ requestDimensions(request)
    requestsActive.inc(dims)
    requests.inc(dims)

    val entity = request.entity match {
      case data: HttpEntity.Strict =>
        requestsSize.update(data.contentLength, dims)
        data
      case data: HttpEntity.Default =>
        requestsSize.update(data.contentLength, dims)
        data
      case data: HttpEntity.Chunked =>
        val collectSizeSink = Flow[ByteString]
          .map(_.length)
          .fold(0L)(_ + _)
          .to(Sink.foreach(size => requestsSize.update(size, dims)))
        data.transformDataBytes(Flow[ByteString].alsoTo(collectSizeSink))
    }

    // modify the request
    request
      .addAttribute(TraceTimestampKey, start)
      .withEntity(entity)
  }

  override def onResponse(request: HttpRequest, response: HttpResponse): HttpResponse = {
    val start    = request.attribute(TraceTimestampKey).get
    val reqDims  = settings.serverDimensions ++ requestDimensions(request)
    val respDims = reqDims ++ responseDimensions(response)

    requestsActive.dec(reqDims)
    responses.inc(respDims)
    if (settings.defineError(response)) responsesErrors.inc(respDims)
    response.entity match {
      case data: HttpEntity.Strict =>
        responsesSize.update(data.contentLength, respDims)
        responsesDuration.observe(Deadline.now - start, respDims)
        response
      case data: HttpEntity.Default =>
        responsesSize.update(data.contentLength, respDims)
        responsesDuration.observe(Deadline.now - start, respDims)
        response
      case _: HttpEntity.Chunked | _: HttpEntity.CloseDelimited =>
        val collectSizeSink = Flow[ByteString]
          .map(_.length)
          .fold(0L)(_ + _)
          .to(Sink.foreach { size =>
            responsesSize.update(size, respDims)
            responsesDuration.observe(Deadline.now - start, respDims)
          })
        response.transformEntityDataBytes(Flow[ByteString].alsoTo(collectSizeSink))
    }
  }

  override def onFailure(request: HttpRequest, cause: Throwable): Throwable = {
    val dims = settings.serverDimensions ++ requestDimensions(request)
    requestsActive.dec(dims)
    requestsFailures.inc(dims)
    cause
  }

  override def onConnection(): Unit = {
    val dims = settings.serverDimensions
    connections.inc(dims)
    connectionsActive.inc(dims)
  }

  override def onDisconnection(): Unit = {
    val dims = settings.serverDimensions
    connectionsActive.dec(dims)
  }

  // --------------------------------------------------------------------------------------------------------------------
  // requests
  // --------------------------------------------------------------------------------------------------------------------
  def requests: Counter

  def requestsActive: Gauge

  def requestsFailures: Counter

  def requestsSize: Histogram

  // --------------------------------------------------------------------------------------------------------------------
  // responses
  // --------------------------------------------------------------------------------------------------------------------
  def responses: Counter

  def responsesErrors: Counter

  def responsesDuration: Timer

  def responsesSize: Histogram

  // --------------------------------------------------------------------------------------------------------------------
  // Connections
  // --------------------------------------------------------------------------------------------------------------------
  def connections: Counter

  def connectionsActive: Gauge
}

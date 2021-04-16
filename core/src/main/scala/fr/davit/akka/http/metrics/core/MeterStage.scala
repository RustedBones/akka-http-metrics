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

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, BidiShape, Inlet, Outlet}

import java.util.UUID
import scala.collection.mutable

object MeterStage {
  val PrematureCloseException = new IllegalStateException("Stream completed prematurely")

  private[core] val MissingTraceIdException = new NoSuchElementException(
    s"Request is missing '${HttpMetrics.TraceId.name}' attribute. This could be possibly caused by implicit conversion " +
      "conflict during HTTP server creation. Try to use explicit conversion: " +
      "replace Http().newMeteredServerAt(...).bindFlow(routes) " +
      "with Http().newMeteredServerAt(...).bindFlow(HttpMetrics.metricsRouteToFlow(routes))"
  )
}

private[metrics] class MeterStage(metricsHandler: HttpMetricsHandler)
    extends GraphStage[BidiShape[HttpRequest, HttpRequest, HttpResponse, HttpResponse]] {

  import MeterStage._

  private val requestIn   = Inlet[HttpRequest]("MeterStage.requestIn")
  private val requestOut  = Outlet[HttpRequest]("MeterStage.requestOut")
  private val responseIn  = Inlet[HttpResponse]("MeterStage.responseIn")
  private val responseOut = Outlet[HttpResponse]("MeterStage.responseOut")

  override def initialAttributes = Attributes.name("MeterStage")

  val shape = new BidiShape(requestIn, requestOut, responseIn, responseOut)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    private val pending = mutable.Map.empty[UUID, HttpRequest]
    private var failure = Option.empty[Throwable]

    override def preStart(): Unit = {
      metricsHandler.onConnection()
    }

    override def postStop(): Unit = {
      val cause = failure.getOrElse(PrematureCloseException)
      pending.values.foreach(metricsHandler.onFailure(_, cause))
      metricsHandler.onDisconnection()
    }

    val requestHandler = new InHandler with OutHandler {

      override def onPush(): Unit = {
        val request = grab(requestIn)
        val id = request.attribute(HttpMetrics.TraceId) match {
          case Some(id) => id
          case _        => throw MissingTraceIdException
        }
        pending += id -> request
        push(requestOut, metricsHandler.onRequest(request))
      }
      override def onPull(): Unit = pull(requestIn)

      override def onUpstreamFinish(): Unit                   = complete(requestOut)
      override def onUpstreamFailure(ex: Throwable): Unit     = fail(requestOut, ex)
      override def onDownstreamFinish(cause: Throwable): Unit = cancel(requestIn)
    }

    val responseHandler = new InHandler with OutHandler {

      override def onPush(): Unit = {
        val response = grab(responseIn)
        val id       = response.getAttribute(HttpMetrics.TraceId).get
        val request  = pending.remove(id).get
        push(responseOut, metricsHandler.onResponse(request, response))
      }
      override def onPull(): Unit = pull(responseIn)

      override def onUpstreamFinish(): Unit = {
        complete(responseOut)
      }

      override def onUpstreamFailure(ex: Throwable): Unit = {
        failure = Some(ex)
        fail(responseOut, ex)
      }

      override def onDownstreamFinish(cause: Throwable): Unit = {
        failure = Some(cause)
        cancel(responseIn)
      }
    }

    setHandlers(requestIn, requestOut, requestHandler)
    setHandlers(responseIn, responseOut, responseHandler)
  }
}

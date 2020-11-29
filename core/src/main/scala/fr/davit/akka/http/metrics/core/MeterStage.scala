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
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{Attributes, BidiShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.collection.mutable
import scala.concurrent.Promise

private[metrics] class MeterStage(metricsHandler: HttpMetricsHandler)
    extends GraphStage[BidiShape[HttpRequest, HttpRequest, HttpResponse, HttpResponse]] {
  private val requestIn   = Inlet[HttpRequest]("MeterStage.requestIn")
  private val requestOut  = Outlet[HttpRequest]("MeterStage.requestOut")
  private val responseIn  = Inlet[HttpResponse]("MeterStage.responseIn")
  private val responseOut = Outlet[HttpResponse]("MeterStage.responseOut")

  override def initialAttributes = Attributes.name("MeterStage")

  val shape = new BidiShape(requestIn, requestOut, responseIn, responseOut)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    val completion: Promise[Done] = Promise()
    // Use a FIFO structure to store response promises
    // All routes are converted to flow with a mapAsync(1) so order is respected
    val pending: mutable.Queue[Promise[HttpResponse]] = mutable.Queue.empty

    override def preStart(): Unit = {
      metricsHandler.onConnection(completion.future)(materializer)
    }

    override def postStop(): Unit = {
      completion.success(Done)
    }

    val requestHandler = new InHandler with OutHandler {
      override def onPush(): Unit = {
        val request = grab(requestIn)
        val promise = Promise[HttpResponse]()
        metricsHandler.onRequest(request, promise.future)(materializer)
        pending.enqueue(promise)
        push(requestOut, request)
      }
      override def onPull(): Unit = pull(requestIn)

      override def onUpstreamFinish(): Unit                   = complete(requestOut) // do not completeStage and flush stream
      override def onUpstreamFailure(ex: Throwable): Unit     = fail(requestOut, ex)
      override def onDownstreamFinish(cause: Throwable): Unit = cancel(requestIn)
    }

    val responseHandler = new InHandler with OutHandler {
      override def onPush(): Unit = {
        val promise  = pending.dequeue()
        val response = grab(responseIn)
        promise.success(response)
        push(responseOut, response)
      }
      override def onPull(): Unit = pull(responseIn)

      override def onUpstreamFinish(): Unit = {
        pending.foreach(_.failure(new IllegalStateException("Server stopped with pending requests")))
        complete(responseOut)
      }
      override def onUpstreamFailure(ex: Throwable): Unit = {
        pending.foreach(_.failure(ex))
        fail(responseOut, ex)
      }
      override def onDownstreamFinish(cause: Throwable): Unit = {
        pending.foreach(_.failure(cause))
        cancel(responseIn)
      }
    }

    setHandlers(requestIn, requestOut, requestHandler)
    setHandlers(responseIn, responseOut, responseHandler)
  }
}

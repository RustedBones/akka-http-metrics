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
      super.preStart()
      metricsHandler.onConnection(completion.future)(materializer.executionContext)
    }

    setHandler(
      requestIn,
      new InHandler {
        override def onPush(): Unit = {
          val request = grab(requestIn)
          val promise = Promise[HttpResponse]()
          metricsHandler.onRequest(request, promise.future)(materializer.executionContext)
          pending.enqueue(promise)
          push(requestOut, request)
        }
      }
    )
    setHandler(requestOut, new OutHandler {
      override def onPull(): Unit = pull(requestIn)
    })
    setHandler(
      responseIn,
      new InHandler {
        override def onPush(): Unit = {
          val promise  = pending.dequeue()
          val response = grab(responseIn)
          promise.success(response)
          push(responseOut, response)
        }

        override def onUpstreamFinish(): Unit = {
          completion.success(Done)
          super.onUpstreamFinish()
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          completion.success(Done)
          pending.foreach(_.failure(ex))
          super.onUpstreamFailure(ex)
        }
      }
    )
    setHandler(responseOut, new OutHandler {
      override def onPull(): Unit = pull(responseIn)
    })
  }
}

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

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ClosedShape
import akka.stream.scaladsl.{GraphDSL, RunnableGraph}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.util.UUID

class MeterStageSpec
    extends TestKit(ActorSystem("MeterStageSpec"))
    with AnyFlatSpecLike
    with Matchers
    with MockFactory
    with ScalaFutures {

  val traceId        = UUID.fromString("00000000-0000-0000-0000-000000000000")
  val tracedRequest  = HttpRequest().addAttribute(HttpMetrics.TraceId, traceId)
  val tracedResponse = HttpResponse().addAttribute(HttpMetrics.TraceId, traceId)

  trait Fixture {
    val handler = mock[HttpMetricsHandler]

    (handler.onConnection _)
      .expects()
      .returns((): Unit)

    val (requestIn, requestOut, responseIn, responseOut) = RunnableGraph
      .fromGraph(
        GraphDSL.createGraph(
          TestSource.probe[HttpRequest],
          TestSink.probe[HttpRequest],
          TestSource.probe[HttpResponse],
          TestSink.probe[HttpResponse]
        )((_, _, _, _)) { implicit builder => (reqIn, reqOut, respIn, respOut) =>
          import GraphDSL.Implicits._
          val meter = builder.add(new MeterStage(handler))

          reqIn ~> meter.in1
          meter.out1 ~> reqOut
          respIn ~> meter.in2
          meter.out2 ~> respOut
          ClosedShape
        }
      )
      .run()

    // simulate downstream demand
    responseOut.request(1)
    requestOut.request(1)
  }

  "MeterStage" should "call onConnection on materialization and onDisconnection once terminated" in new Fixture {
    (handler.onDisconnection _)
      .expects()
      .returns((): Unit)

    requestIn.sendComplete()
    requestOut.expectComplete()

    responseIn.sendComplete()
    responseOut.expectComplete()
  }

  it should "call onRequest wen request is offered" in new Fixture {
    (handler.onRequest _)
      .expects(tracedRequest)
      .returns(tracedRequest)

    requestIn.sendNext(tracedRequest)
    requestOut.expectNext() shouldBe tracedRequest

    (handler.onResponse _)
      .expects(tracedRequest, tracedResponse)
      .returns(tracedResponse)

    responseIn.sendNext(tracedResponse)
    responseOut.expectNext() shouldBe tracedResponse
  }

  it should "flush the stream before stopping" in new Fixture {
    (handler.onRequest _)
      .expects(tracedRequest)
      .returns(tracedRequest)

    requestIn.sendNext(tracedRequest)
    requestOut.expectNext() shouldBe tracedRequest

    // close request side
    requestIn.sendComplete()
    requestOut.expectComplete()

    // response should still be accepted
    (handler.onResponse _)
      .expects(tracedRequest, tracedResponse)
      .returns(tracedResponse)

    responseIn.sendNext(tracedResponse)
    responseOut.expectNext() shouldBe tracedResponse
  }

  it should "throw a NoSuchElementException if trace-id attribute is missing in request" in new Fixture {
    val requestWithoutTraceId = HttpRequest()

    requestIn.sendNext(requestWithoutTraceId)

    val error = requestIn.expectCancellationWithCause[NoSuchElementException]()
    requestOut.expectError(error)
    responseIn.expectCancellationWithCause[NoSuchElementException]()
    responseOut.expectError(error)
  }

  it should "throw a NoSuchElementException if trace-id attribute is missing in response" in new Fixture {
    val responseWithoutTraceId = HttpResponse()

    responseIn.sendNext(responseWithoutTraceId)

    val error = requestIn.expectCancellationWithCause[NoSuchElementException]()
    requestOut.expectError(error)
    responseIn.expectCancellationWithCause[NoSuchElementException]()
    responseOut.expectError(error)
  }

  it should "throw a NoSuchElementException if trace-id is unknown on response" in new Fixture {
    responseIn.sendNext(tracedResponse)

    val error = requestIn.expectCancellationWithCause[NoSuchElementException]()
    error.getMessage should include(traceId.toString)
    requestOut.expectError(error)
    responseIn.expectCancellationWithCause[NoSuchElementException]()
    responseOut.expectError(error)
  }

  it should "propagate error from request in" in new Fixture {
    (handler.onRequest _)
      .expects(tracedRequest)
      .returns(tracedRequest)

    requestIn.sendNext(tracedRequest)
    requestOut.expectNext() shouldBe tracedRequest

    val error = new Exception("BOOM!")
    requestIn.sendError(error)
    requestOut.expectError(error)
  }

  it should "propagate error from request out" in new Fixture {
    (handler.onRequest _)
      .expects(tracedRequest)
      .returns(tracedRequest)

    requestIn.sendNext(tracedRequest)
    requestOut.expectNext() shouldBe tracedRequest

    val error = new Exception("BOOM!")
    requestOut.cancel(error)
    requestIn.expectCancellation()
  }

  it should "terminate and fail pending" in new Fixture {
    (handler.onRequest _)
      .expects(tracedRequest)
      .returns(tracedRequest)

    requestIn.sendNext(tracedRequest)
    requestIn.sendComplete()
    requestOut.expectNext() shouldBe tracedRequest
    requestOut.expectComplete()

    (handler.onFailure _)
      .expects(tracedRequest, MeterStage.PrematureCloseException)
      .returns(MeterStage.PrematureCloseException)

    responseIn.sendComplete()
    responseOut.expectComplete()
  }

  it should "propagate error from response in and fail pending" in new Fixture {
    (handler.onRequest _)
      .expects(tracedRequest)
      .returns(tracedRequest)

    requestIn.sendNext(tracedRequest)
    requestIn.sendComplete()
    requestOut.expectNext() shouldBe tracedRequest
    requestOut.expectComplete()

    val error = new Exception("BOOM!")
    (handler.onFailure _)
      .expects(tracedRequest, error)
      .returns(error)

    responseIn.sendError(error)
    responseOut.expectError(error)
  }

  it should "propagate error from response out and fail pending" in new Fixture {
    (handler.onRequest _)
      .expects(tracedRequest)
      .returns(tracedRequest)

    requestIn.sendNext(tracedRequest)
    requestIn.sendComplete()
    requestOut.expectNext() shouldBe tracedRequest
    requestOut.expectComplete()

    val error = new Exception("BOOM!")
    (handler.onFailure _)
      .expects(tracedRequest, error)
      .returns(error)

    responseOut.cancel(error)
    responseIn.expectCancellation()
  }
}

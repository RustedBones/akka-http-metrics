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
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{GraphDSL, RunnableGraph}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.{ClosedShape, Materializer}
import akka.testkit.TestKit
import org.scalamock.matchers.ArgCapture.CaptureOne
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

class MeterStageSpec
    extends TestKit(ActorSystem("HttpMetricsSpec"))
    with AnyFlatSpecLike
    with Matchers
    with MockFactory
    with ScalaFutures {

  trait Fixture {
    val handler           = mock[HttpMetricsHandler]
    val terminationFuture = CaptureOne[Future[Done]]()

    (handler
      .onConnection(_: Future[Done])(_: Materializer))
      .expects(capture(terminationFuture), *)
      .returns((): Unit)

    val (requestIn, requestOut, responseIn, responseOut) = RunnableGraph
      .fromGraph(
        GraphDSL.create(
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

  "MeterStage" should "call onConnection on materialization and complete the future once terminated" in new Fixture {
    requestIn.sendComplete()
    requestOut.expectComplete()

    responseIn.sendComplete()
    responseOut.expectComplete()

    terminationFuture.value.futureValue shouldBe Done
  }

  it should "call onRequest wen request is offered" in new Fixture {
    val request        = HttpRequest()
    val response       = HttpResponse()
    val responseFuture = CaptureOne[Future[HttpResponse]]()

    (handler
      .onRequest(_: HttpRequest, _: Future[HttpResponse])(_: Materializer))
      .expects(request, capture(responseFuture), *)
      .returns(Future.successful(response))

    requestIn.sendNext(request)
    requestOut.expectNext() shouldBe request

    responseFuture.value.isCompleted shouldBe false

    responseIn.sendNext(response)
    responseOut.expectNext() shouldBe response

    responseFuture.value.futureValue shouldBe response
  }

  it should "flush the stream before stopping" in new Fixture {
    val request  = HttpRequest()
    val response = HttpResponse()
    val actual   = CaptureOne[Future[HttpResponse]]()

    (handler
      .onRequest(_: HttpRequest, _: Future[HttpResponse])(_: Materializer))
      .expects(request, capture(actual), *)
      .returns(Future.successful(response))

    requestIn.sendNext(request)
    requestOut.expectNext() shouldBe request

    actual.value.isCompleted shouldBe false

    // close request side
    requestIn.sendComplete()
    requestOut.expectComplete()

    // response should still be accepted
    responseIn.sendNext(response)
    responseOut.expectNext() shouldBe response

    actual.value.futureValue shouldBe response
  }

  it should "propagate error from request in" in new Fixture {
    val request  = HttpRequest()
    val response = HttpResponse()
    val actual   = CaptureOne[Future[HttpResponse]]()

    (handler
      .onRequest(_: HttpRequest, _: Future[HttpResponse])(_: Materializer))
      .expects(request, capture(actual), *)
      .returns(Future.successful(response))

    requestIn.sendNext(request)
    requestOut.expectNext() shouldBe request

    val error = new Exception("BOOM!")
    requestIn.sendError(error)
    requestOut.expectError(error)
  }

  it should "propagate error from request out" in new Fixture {
    val request  = HttpRequest()
    val response = HttpResponse()
    val actual   = CaptureOne[Future[HttpResponse]]()

    (handler
      .onRequest(_: HttpRequest, _: Future[HttpResponse])(_: Materializer))
      .expects(request, capture(actual), *)
      .returns(Future.successful(response))

    requestIn.sendNext(request)
    requestOut.expectNext() shouldBe request

    val error = new Exception("BOOM!")
    requestOut.cancel(error)
    requestIn.expectCancellation()
  }

  it should "propagate error from response in and complete pending" in new Fixture {
    val request  = HttpRequest()
    val response = HttpResponse()
    val actual   = CaptureOne[Future[HttpResponse]]()

    (handler
      .onRequest(_: HttpRequest, _: Future[HttpResponse])(_: Materializer))
      .expects(request, capture(actual), *)
      .returns(Future.successful(response))

    requestIn.sendNext(request)
    requestOut.expectNext() shouldBe request

    val error = new Exception("BOOM!")
    responseIn.sendError(error)
    responseOut.expectError(error)

    actual.value.failed.futureValue shouldBe error
  }

  it should "propagate error from response out and complete pending" in new Fixture {
    val request  = HttpRequest()
    val response = HttpResponse()
    val actual   = CaptureOne[Future[HttpResponse]]()

    (handler
      .onRequest(_: HttpRequest, _: Future[HttpResponse])(_: Materializer))
      .expects(request, capture(actual), *)
      .returns(Future.successful(response))

    requestIn.sendNext(request)
    requestOut.expectNext() shouldBe request

    val error = new Exception("BOOM!")
    responseOut.cancel(error)
    responseIn.expectCancellation()

    actual.value.failed.futureValue shouldBe error
  }
}

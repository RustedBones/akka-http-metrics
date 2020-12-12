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

package fr.davit.akka.http.metrics.core.scaladsl.server

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext, RouteResult}
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsRoute._
import fr.davit.akka.http.metrics.core.{HttpMetrics, HttpMetricsHandler}
import org.scalamock.matchers.ArgCapture.CaptureOne
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

class HttpMetricsRouteSpec
    extends TestKit(ActorSystem("HttpMetricsRouteSpec"))
    with AnyFlatSpecLike
    with Matchers
    with ScalaFutures
    with MockFactory
    with BeforeAndAfterAll {

  import Directives._

  implicit val ec: ExecutionContext = system.dispatcher

  abstract class Fixture[T] {
    val metricsHandler = mock[HttpMetricsHandler]
    val server         = mockFunction[RequestContext, Future[RouteResult]]

    (metricsHandler.onConnection _)
      .expects()
      .returns((): Unit)

    (metricsHandler.onDisconnection _)
      .expects()
      .returns((): Unit)

    val (source, sink) = TestSource
      .probe[HttpRequest]
      .via(server.recordMetricsImpl(metricsHandler))
      .toMat(TestSink.probe[HttpResponse])(Keep.both)
      .run()
  }

  override def afterAll(): Unit = {
    shutdown()
    super.afterAll()
  }

  "HttpMetricsRoute" should "call the metrics handler on connection" in {
    val metricsHandler = mock[HttpMetricsHandler]
    val server         = mockFunction[RequestContext, Future[RouteResult]]

    (metricsHandler.onConnection _)
      .expects()
      .returns((): Unit)

    val (source, sink) = TestSource
      .probe[HttpRequest]
      .via(server.recordMetricsImpl(metricsHandler))
      .toMat(TestSink.probe[HttpResponse])(Keep.both)
      .run()

    sink.request(1)
    source.sendComplete()
    sink.expectComplete()
  }

  it should "call the metrics handler on handled requests" in new Fixture {
    val request  = CaptureOne[HttpRequest]()
    val response = CaptureOne[HttpResponse]()
    (metricsHandler.onRequest _)
      .expects(capture(request))
      .onCall { req: HttpRequest =>
        req
      }

    server
      .expects(*)
      .onCall(complete(StatusCodes.OK))

    (metricsHandler.onResponse _)
      .expects(*, capture(response))
      .onCall { (_: HttpRequest, resp: HttpResponse) =>
        resp
      }

    sink.request(1)
    source.sendNext(HttpRequest())
    sink.expectNext()

    source.sendComplete()
    sink.expectComplete()

    val traceId = request.value.attribute(HttpMetrics.TraceId)
    val traceTs = request.value.attribute(HttpMetrics.TraceTimestamp)
    traceId shouldBe defined
    traceTs shouldBe defined

    val expected = Marshal(StatusCodes.OK)
      .to[HttpResponse]
      .futureValue
      .addAttribute(HttpMetrics.TraceId, traceId.get)
    response.value shouldBe expected
  }

  it should "call the metrics handler on rejected requests" in new Fixture {
    val request  = CaptureOne[HttpRequest]()
    val response = CaptureOne[HttpResponse]()
    (metricsHandler.onRequest _)
      .expects(capture(request))
      .onCall { req: HttpRequest =>
        req
      }

    server
      .expects(*)
      .onCall(reject)

    (metricsHandler.onResponse _)
      .expects(*, capture(response))
      .onCall { (_: HttpRequest, resp: HttpResponse) =>
        resp
      }

    sink.request(1)
    source.sendNext(HttpRequest())
    sink.expectNext()

    source.sendComplete()
    sink.expectComplete()

    val traceId = request.value.attribute(HttpMetrics.TraceId).get

    val expected = Marshal(StatusCodes.NotFound -> "The requested resource could not be found.")
      .to[HttpResponse]
      .futureValue
      .addAttribute(HttpMetrics.TraceId, traceId)
      .addAttribute(HttpMetrics.PathLabel, "unhandled")
    response.value shouldBe expected
  }

  it should "call the metrics handler on error requests" in new Fixture {
    val request  = CaptureOne[HttpRequest]()
    val response = CaptureOne[HttpResponse]()
    (metricsHandler.onRequest _)
      .expects(capture(request))
      .onCall { req: HttpRequest =>
        req
      }

    server
      .expects(*)
      .onCall(failWith(new Exception("BOOM!")))

    (metricsHandler.onResponse _)
      .expects(*, capture(response))
      .onCall { (_: HttpRequest, resp: HttpResponse) =>
        resp
      }

    sink.request(1)
    source.sendNext(HttpRequest())
    sink.expectNext()

    source.sendComplete()
    sink.expectComplete()

    val expected = Marshal(StatusCodes.InternalServerError)
      .to[HttpResponse]
      .futureValue
      .addAttribute(HttpMetrics.TraceId, request.value.attribute(HttpMetrics.TraceId).get)
      .addAttribute(HttpMetrics.PathLabel, "unhandled")
    response.value shouldBe expected
  }
}

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

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext, RouteResult}
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import fr.davit.akka.http.metrics.core.HttpMetricsHandler
import fr.davit.akka.http.metrics.core.scaladsl.model.PathLabelHeader
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsRoute._
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class HttpMetricsRouteSpec
    extends TestKit(ActorSystem("HttpMetricsRouteSpec"))
    with AnyFlatSpecLike
    with Matchers
    with MockFactory
    with BeforeAndAfterAll {

  import Directives._

  abstract class Fixture[T] {
    val metricsHandler = mock[HttpMetricsHandler]
    val server         = mockFunction[RequestContext, Future[RouteResult]]

    (metricsHandler
      .onConnection(_: Future[Done])(_: ExecutionContext))
      .expects(*, *)
      .returns((): Unit)

    val (source, sink) = TestSource
      .probe[HttpRequest]
      .via(server.recordMetrics(metricsHandler))
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

    (metricsHandler
      .onConnection(_: Future[Done])(_: ExecutionContext))
      .expects(*, *)
      .returns((): Unit)

    val (source, sink) = TestSource
      .probe[HttpRequest]
      .via(server.recordMetrics(metricsHandler))
      .toMat(TestSink.probe[HttpResponse])(Keep.both)
      .run()

    sink.request(1)
    source.sendComplete()
    sink.expectComplete()
  }

  it should "call the metrics handler on handled requests" in new Fixture {
    val request  = HttpRequest()
    val response = Marshal(StatusCodes.OK).to[HttpResponse]

    server
      .expects(*)
      .onCall(complete(StatusCodes.OK))
    (metricsHandler
      .onRequest(_: HttpRequest, _: Future[HttpResponse])(_: ExecutionContext))
      .expects(request, response, *)
      .returns((): Unit)

    sink.request(1)
    source.sendNext(request)
    sink.expectNext()
  }

  it should "call the metrics handler on rejected requests" in new Fixture {
    val request = HttpRequest()

    val response = Marshal(StatusCodes.NotFound -> "The requested resource could not be found.")
      .to[HttpResponse]
      .map(_.withHeaders(PathLabelHeader("unhandled")))

    server
      .expects(*)
      .onCall(reject)
    (metricsHandler
      .onRequest(_: HttpRequest, _: Future[HttpResponse])(_: ExecutionContext))
      .expects(request, response, *)
      .returns((): Unit)

    sink.request(1)
    source.sendNext(request)
    sink.expectNext()
  }

  it should "call the metrics handler on error requests" in new Fixture {
    val request = HttpRequest()

    val response = Marshal(StatusCodes.InternalServerError)
      .to[HttpResponse]
      .map(_.withHeaders(PathLabelHeader("unhandled")))

    server
      .expects(*)
      .onCall(failWith(new Exception("BOOM!")))
    (metricsHandler
      .onRequest(_: HttpRequest, _: Future[HttpResponse])(_: ExecutionContext))
      .expects(request, response, *)
      .returns((): Unit)

    sink.request(1)
    source.sendNext(request)
    sink.expectNext()
  }
}

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
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext, RouteResult}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{PathDimension, StatusGroupDimension}
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.StatusGroupDimension.StatusGroup
import fr.davit.akka.http.metrics.core.TestRegistry
import fr.davit.akka.http.metrics.core.scaladsl.model.SegmentLabelHeader
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsRoute._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

class HttpMetricsRouteSpec
    extends TestKit(ActorSystem("HttpMetricsRouteSpec"))
    with FlatSpecLike
    with Matchers
    with Eventually
    with ScalaFutures
    with MockFactory
    with BeforeAndAfterAll {

  import Directives._

  implicit val _            = system
  implicit val materializer = ActorMaterializer()

  abstract class Fixture[T](testField: TestRegistry => T, settings: HttpMetricsSettings = HttpMetricsSettings.default) {
    implicit val registry = new TestRegistry

    val server = mockFunction[RequestContext, Future[RouteResult]]

    val (source, sink) = TestSource
      .probe[HttpRequest]
      .via(server.recordMetrics(registry, settings))
      .toMat(TestSink.probe[HttpResponse])(Keep.both)
      .run()

    def actual: T = testField(registry)
  }

  override def afterAll(): Unit = {
    shutdown()
    super.afterAll()
  }

  "HttpMetricsRoute" should "compute the number of requests" in new Fixture(_.requests) {
    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.OK))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value() shouldBe 1

    sink.request(1)
    server.expects(*).onCall(reject)
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value() shouldBe 2

    sink.request(1)
    server.expects(*).onCall(failWith(new Exception("BOOM!")))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value() shouldBe 3
  }

  it should "compute the number of errors" in new Fixture(_.errors) {
    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.OK))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value() shouldBe 0

    sink.request(1)
    server.expects(*).onCall(reject)
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value() shouldBe 0

    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.InternalServerError))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value() shouldBe 1

    sink.request(1)
    server.expects(*).onCall(failWith(new Exception("BOOM!")))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value() shouldBe 2
  }

  it should "compute the number of active requests" in new Fixture(_.active) {
    val promise = Promise[StatusCode]()

    sink.request(1)
    server.expects(*).onCall(complete(promise.future))
    source.sendNext(HttpRequest())

    // wait for the request to be sent to the handler
    eventually(actual.value() shouldBe 1)

    promise.success(StatusCodes.OK)
    sink.expectNext()
    actual.value() shouldBe 0
  }

  it should "compute the requests time" in new Fixture(_.duration) {
    val promise  = Promise[StatusCode]()
    val duration = 500.millis

    sink.request(1)
    server.expects(*).onCall(complete(promise.future))
    source.sendNext(HttpRequest())
    system.scheduler.scheduleOnce(duration)(promise.success(StatusCodes.OK))(system.dispatcher)
    sink.expectNext(duration * 2)
    actual.values().head should be > duration
  }

  it should "compute the requests size" in new Fixture(_.receivedBytes) {
    val data = "This is the request content"

    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.OK))
    source.sendNext(HttpRequest(entity = HttpEntity(data)))
    sink.expectNext()
    actual.values().head shouldBe data.getBytes.length
  }

  it should "compute the response size" in new Fixture(_.sentBytes) {
    val data = "This is the response content"

    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.OK -> data))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.values().head shouldBe data.getBytes.length
  }

  it should "compute the number of active connections" in {
    implicit val registry = new TestRegistry
    val server = mockFunction[RequestContext, Future[RouteResult]]

    val stream = Source.maybe.via(server.recordMetrics(registry)).toMat(Sink.ignore)(Keep.both)
    val conns  = (0 until 3).map(_ => stream.run())
    registry.connected.value() shouldBe 3

    conns.map { case (c, _) => c.success(None) }
    Future.sequence(conns.map { case (_, completion) => completion }).futureValue
    registry.connected.value() shouldBe 0
  }

  it should "compute the number of connections" in  {
    implicit val registry = new TestRegistry
    val server = mockFunction[RequestContext, Future[RouteResult]]

    val stream = Source((0 until 5).map(_ => HttpRequest())).via(server.recordMetrics(registry)).toMat(Sink.ignore)(Keep.right)
    val completions = (0 until 3).map(_ => stream.run())
    Future.sequence(completions).futureValue
    registry.connections.value() shouldBe 3
  }

  it should "add status code dimension when enabled" in new Fixture(_.responses, HttpMetricsSettings.default.withIncludeStatusDimension(true)) {
    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.OK))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value(Seq(StatusGroupDimension(StatusGroup.`2xx`))) shouldBe 1
    actual.value(Seq(StatusGroupDimension(StatusGroup.`3xx`))) shouldBe 0
    actual.value(Seq(StatusGroupDimension(StatusGroup.`4xx`))) shouldBe 0
    actual.value(Seq(StatusGroupDimension(StatusGroup.`5xx`))) shouldBe 0
  }

  it should "add path dimension when enabled" in new Fixture(_.responses, HttpMetricsSettings.default.withIncludePathDimension(true)) {
    val path = "/this/is/the/path"
    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.OK))
    source.sendNext(HttpRequest().withUri(path))
    sink.expectNext()
    actual.value(Seq(PathDimension(path))) shouldBe 1
    actual.value(Seq(PathDimension("/other/path"))) shouldBe 0
  }

  it should "correctly replace segment labels in path" in new Fixture(_.responses, HttpMetricsSettings.default.withIncludePathDimension(true)) {
    val path = "/this/is/the/path"
    sink.request(1)
    server.expects(*).onCall(respondWithHeader(SegmentLabelHeader(2, 6, "/label"))(complete(StatusCodes.OK)))
    source.sendNext(HttpRequest().withUri(path))
    sink.expectNext()
    actual.value(Seq(PathDimension("/this/label/path"))) shouldBe 1
    actual.value(Seq(PathDimension("/other/path"))) shouldBe 0
  }

  it should "add unhandled path dimension when request is rejected" in new Fixture(_.responses, HttpMetricsSettings.default.withIncludePathDimension(true)) {
    val path = "/this/is/the/path"
    sink.request(1)
    server.expects(*).onCall(reject)
    source.sendNext(HttpRequest().withUri(path))
    val response = sink.expectNext()
    actual.value(Seq(PathDimension("unhandled"))) shouldBe 1
    actual.value(Seq(PathDimension(path))) shouldBe 0
  }

  it should "not leak custom headers" in new Fixture(_.sentBytes) {
    sink.request(1)
    server.expects(*).onCall(respondWithHeader(SegmentLabelHeader(0, 1, "/label"))(complete(StatusCodes.OK)))
    source.sendNext(HttpRequest())
    val response = sink.expectNext()
    response.header[SegmentLabelHeader] shouldBe empty
  }
}

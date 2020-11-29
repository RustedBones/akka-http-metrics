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
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import akka.util.ByteString
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{MethodDimension, PathDimension, StatusGroupDimension}
import fr.davit.akka.http.metrics.core.scaladsl.model.PathLabelHeader
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

class HttpMetricsRegistrySpec
    extends TestKit(ActorSystem("HttpMetricsRegistrySpec"))
    with AnyFlatSpecLike
    with Matchers
    with Eventually {

  implicit val materializer: Materializer = Materializer(system)

  final case object TestDimension extends Dimension {
    override def key: String   = "env"
    override def value: String = "test"
  }

  abstract class Fixture(settings: HttpMetricsSettings = TestRegistry.settings) {
    val registry = new TestRegistry(settings)
  }

  "HttpMetricsRegistry" should "compute the number of requests" in new Fixture() {
    registry.requests.value() shouldBe 0
    registry.onRequest(HttpRequest(), Future.successful(HttpResponse()))
    registry.requests.value() shouldBe 1
    registry.onRequest(HttpRequest(), Future.successful(HttpResponse()))
    registry.requests.value() shouldBe 2
  }

  it should "compute the number of errors" in new Fixture() {
    registry.responsesErrors.value() shouldBe 0
    registry.onRequest(HttpRequest(), Future.successful(HttpResponse(StatusCodes.OK)))
    registry.onRequest(HttpRequest(), Future.successful(HttpResponse(StatusCodes.TemporaryRedirect)))
    registry.onRequest(HttpRequest(), Future.successful(HttpResponse(StatusCodes.BadRequest)))
    eventually(registry.responsesErrors.value() shouldBe 0)
    registry.onRequest(HttpRequest(), Future.successful(HttpResponse(StatusCodes.InternalServerError)))
    eventually(registry.responsesErrors.value() shouldBe 1)
  }

  it should "compute the number of active requests" in new Fixture() {
    registry.requestsActive.value() shouldBe 0
    val promise = Promise[HttpResponse]()
    registry.onRequest(HttpRequest(), promise.future)
    registry.onRequest(HttpRequest(), promise.future)
    registry.requestsActive.value() shouldBe 2
    promise.success(HttpResponse())
    eventually(registry.requestsActive.value() shouldBe 0)
  }

  it should "compute the requests time" in new Fixture() {
    val promise  = Promise[HttpResponse]()
    val duration = 500.millis
    registry.responsesDuration.values() shouldBe empty
    registry.onRequest(HttpRequest(), promise.future)
    Thread.sleep(duration.toMillis)
    promise.success(HttpResponse())
    eventually(registry.responsesDuration.values().head should be > duration)
  }

  it should "compute the requests size" in new Fixture() {
    val data = "This is the request content"
    registry.requestsSize.values() shouldBe empty
    registry.onRequest(HttpRequest(entity = data), Future.successful(HttpResponse()))
    eventually(registry.requestsSize.values().head shouldBe data.getBytes.length)
  }

  it should "compute the requests size for streamed data" in new Fixture() {
    val data   = Source(List("a", "b", "c")).map(ByteString.apply)
    val entity = HttpEntity(ContentTypes.`application/octet-stream`, data)
    registry.requestsSize.values() shouldBe empty
    registry.onRequest(HttpRequest(entity = entity), Future.successful(HttpResponse()))
    eventually(registry.requestsSize.values().head shouldBe "abc".getBytes.length)
  }

  it should "compute the response size" in new Fixture() {
    val data = "This is the response content"
    registry.responsesSize.values() shouldBe empty
    registry.onRequest(HttpRequest(), Future.successful(HttpResponse(entity = data)))
    eventually(registry.responsesSize.values().head shouldBe data.getBytes.length)
  }

  it should "compute the response size for streamed data" in new Fixture() {
    val data   = Source(List("a", "b", "c")).map(ByteString.apply)
    val entity = HttpEntity(ContentTypes.`application/octet-stream`, data)
    registry.responsesSize.values() shouldBe empty
    registry.onRequest(HttpRequest(), Future.successful(HttpResponse(entity = entity)))
    eventually(registry.responsesSize.values().head shouldBe "abc".getBytes.length)
  }

  it should "compute the number of connections" in new Fixture() {
    registry.connections.value() shouldBe 0
    registry.onConnection(Future.successful(Done))
    registry.connections.value() shouldBe 1
    registry.onConnection(Future.failed(new Exception("Stream error")))
    registry.connections.value() shouldBe 2
  }

  it should "compute the number of active connections" in new Fixture() {
    registry.connectionsActive.value() shouldBe 0
    val promise = Promise[Done]()
    registry.onConnection(promise.future)
    registry.onConnection(promise.future)
    registry.connectionsActive.value() shouldBe 2
    promise.success(Done)
    eventually(registry.connectionsActive.value() shouldBe 0)
  }

  it should "add status code dimension when enabled" in new Fixture(
    TestRegistry.settings.withIncludeStatusDimension(true)
  ) {
    registry.onRequest(HttpRequest(), Future.successful(HttpResponse()))
    eventually(registry.responses.value(Seq(StatusGroupDimension(StatusCodes.OK))) shouldBe 1)
    eventually(registry.responses.value(Seq(StatusGroupDimension(StatusCodes.Found))) shouldBe 0)
    eventually(registry.responses.value(Seq(StatusGroupDimension(StatusCodes.BadRequest))) shouldBe 0)
    eventually(registry.responses.value(Seq(StatusGroupDimension(StatusCodes.InternalServerError))) shouldBe 0)
  }

  it should "add method dimension when enabled" in new Fixture(
    TestRegistry.settings.withIncludeMethodDimension(true)
  ) {
    registry.onRequest(HttpRequest(), Future.successful(HttpResponse()))
    eventually(registry.responses.value(Seq(MethodDimension(HttpMethods.GET))) shouldBe 1)
    eventually(registry.responses.value(Seq(MethodDimension(HttpMethods.PUT))) shouldBe 0)
  }

  it should "default label dimension to 'unlabelled' when enabled but not annotated by directives" in new Fixture(
    TestRegistry.settings.withIncludePathDimension(true)
  ) {
    registry.onRequest(HttpRequest().withUri("/unlabelled/path"), Future.successful(HttpResponse()))
    eventually(registry.responses.value(Seq(PathDimension("unlabelled"))) shouldBe 1)
    eventually(registry.responses.value(Seq(PathDimension("unhandled"))) shouldBe 0)
  }

  it should "increment proper label dimension" in new Fixture(
    TestRegistry.settings.withIncludePathDimension(true)
  ) {
    val label = "/api"
    registry.onRequest(
      HttpRequest().withUri("/api/path"),
      Future.successful(HttpResponse().withHeaders(PathLabelHeader(label)))
    )
    eventually(registry.responses.value(Seq(PathDimension(label))) shouldBe 1)
    eventually(registry.responses.value(Seq(PathDimension("unlabelled"))) shouldBe 0)
  }

  it should "increment proper custom dimension" in new Fixture(
    TestRegistry.settings.withServerDimensions(List(TestDimension))
  ) {
    registry.onConnection(Future.successful(Done))
    registry.connections.value(Seq(TestDimension)) shouldBe 1

    registry.onRequest(HttpRequest(), Future.successful(HttpResponse()))
    registry.requests.value(Seq(TestDimension)) shouldBe 1
    eventually(registry.responses.value(Seq(TestDimension)) shouldBe 1)
  }
}

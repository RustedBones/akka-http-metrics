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
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import akka.util.ByteString
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{MethodDimension, PathDimension, StatusGroupDimension}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.util.UUID
import scala.concurrent.duration._

class HttpMetricsRegistrySpec
    extends TestKit(ActorSystem("HttpMetricsRegistrySpec"))
    with AnyFlatSpecLike
    with Matchers
    with ScalaFutures {

  implicit val materializer: Materializer = Materializer(system)

  final case object TestDimension extends Dimension {
    override def key: String   = "env"
    override def value: String = "test"
  }

  val traceId0 = UUID.fromString("00000000-0000-0000-0000-000000000000")
  val traceId1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
  val traceId2 = UUID.fromString("00000000-0000-0000-0000-000000000002")

  def tracedRequest(traceId: UUID = traceId0): HttpRequest =
    HttpRequest().addAttribute(HttpMetrics.TracingId, traceId)

  def tracedResponse(traceId: UUID = traceId0): HttpResponse =
    HttpResponse().addAttribute(HttpMetrics.TracingId, traceId)

  abstract class Fixture(settings: HttpMetricsSettings = TestRegistry.settings) {
    val registry = new TestRegistry(settings)
  }

  "HttpMetricsRegistry" should "compute the number of requests" in new Fixture() {
    registry.requests.value() shouldBe 0
    registry.onRequest(tracedRequest())
    registry.requests.value() shouldBe 1
    registry.onRequest(tracedRequest())
    registry.requests.value() shouldBe 2
  }

  it should "compute the number of errors" in new Fixture() {
    registry.onRequest(tracedRequest(traceId0))
    registry.onRequest(tracedRequest(traceId1))
    registry.onRequest(tracedRequest(traceId2))

    registry.responsesErrors.value() shouldBe 0
    registry.onResponse(tracedResponse(traceId0).withStatus(StatusCodes.OK))
    registry.onResponse(tracedResponse(traceId1).withStatus(StatusCodes.BadRequest))
    registry.responsesErrors.value() shouldBe 0
    registry.onResponse(tracedResponse(traceId2).withStatus(StatusCodes.InternalServerError))
    registry.responsesErrors.value() shouldBe 1
  }

  it should "compute the number of active requests" in new Fixture() {
    registry.requestsActive.value() shouldBe 0
    registry.onRequest(tracedRequest(traceId0))
    registry.onRequest(tracedRequest(traceId1))
    registry.requestsActive.value() shouldBe 2
    registry.onResponse(tracedResponse(traceId0))
    registry.onResponse(tracedResponse(traceId1))
    registry.requestsActive.value() shouldBe 0
  }

  it should "compute the requests time" in new Fixture() {
    val duration = 500.millis
    registry.responsesDuration.values() shouldBe empty
    registry.onRequest(tracedRequest())
    Thread.sleep(duration.toMillis)
    registry.onResponse(tracedResponse())
    registry.responsesDuration.values().head should be > duration
  }

  it should "compute the requests size" in new Fixture() {
    val data    = "This is the request content"
    val request = tracedRequest().withEntity(data)
    registry.requestsSize.values() shouldBe empty
    registry.onRequest(request).discardEntityBytes().future().futureValue
    registry.requestsSize.values().head shouldBe data.getBytes.length
  }

  it should "compute the requests size for streamed data" in new Fixture() {
    val data    = Source(List("a", "b", "c")).map(ByteString.apply)
    val request = tracedRequest().withEntity(HttpEntity(ContentTypes.`application/octet-stream`, data))
    registry.requestsSize.values() shouldBe empty
    registry.onRequest(request).discardEntityBytes().future().futureValue
    Thread.sleep(1000)
    registry.requestsSize.values().head shouldBe "abc".getBytes.length
  }

  it should "compute the response size" in new Fixture() {
    val data     = "This is the response content"
    val response = tracedResponse().withEntity(data)
    registry.onRequest(tracedRequest())
    registry.responsesSize.values() shouldBe empty
    registry.onResponse(response).discardEntityBytes().future().futureValue
    registry.responsesSize.values().head shouldBe data.getBytes.length
  }

  it should "compute the response size for streamed data" in new Fixture() {
    val data     = Source(List("a", "b", "c")).map(ByteString.apply)
    val response = tracedResponse().withEntity(HttpEntity(ContentTypes.`application/octet-stream`, data))
    registry.onRequest(tracedRequest())
    registry.responsesSize.values() shouldBe empty
    registry.onResponse(response).discardEntityBytes().future().futureValue
    registry.responsesSize.values().head shouldBe "abc".getBytes.length
  }

  it should "compute the number of connections" in new Fixture() {
    registry.connections.value() shouldBe 0
    registry.onConnection()
    registry.connections.value() shouldBe 1
    registry.onDisconnection()
    registry.onConnection()
    registry.connections.value() shouldBe 2
  }

  it should "compute the number of active connections" in new Fixture() {
    registry.connectionsActive.value() shouldBe 0
    registry.onConnection()
    registry.onConnection()
    registry.connectionsActive.value() shouldBe 2
    registry.onDisconnection()
    registry.onDisconnection()
    registry.connectionsActive.value() shouldBe 0
  }

  it should "add status code dimension when enabled" in new Fixture(
    TestRegistry.settings.withIncludeStatusDimension(true)
  ) {
    registry.onRequest(tracedRequest())
    registry.onResponse(tracedResponse())
    registry.responses.value(Seq(StatusGroupDimension(StatusCodes.OK))) shouldBe 1
    registry.responses.value(Seq(StatusGroupDimension(StatusCodes.Found))) shouldBe 0
    registry.responses.value(Seq(StatusGroupDimension(StatusCodes.BadRequest))) shouldBe 0
    registry.responses.value(Seq(StatusGroupDimension(StatusCodes.InternalServerError))) shouldBe 0
  }

  it should "add method dimension when enabled" in new Fixture(
    TestRegistry.settings.withIncludeMethodDimension(true)
  ) {
    registry.onRequest(tracedRequest())
    registry.onResponse(tracedResponse())
    registry.responses.value(Seq(MethodDimension(HttpMethods.GET))) shouldBe 1
    registry.responses.value(Seq(MethodDimension(HttpMethods.PUT))) shouldBe 0
  }

  it should "default label dimension to 'unlabelled' when enabled but not annotated by directives" in new Fixture(
    TestRegistry.settings.withIncludePathDimension(true)
  ) {
    registry.onRequest(tracedRequest().withUri("/unlabelled/path"))
    registry.onResponse(tracedResponse())
    registry.responses.value(Seq(PathDimension("unlabelled"))) shouldBe 1
    registry.responses.value(Seq(PathDimension("unhandled"))) shouldBe 0
  }

  it should "increment proper label dimension" in new Fixture(
    TestRegistry.settings.withIncludePathDimension(true)
  ) {
    val label = "/api"
    registry.onRequest(tracedRequest().withUri("/api/path"))
    registry.onResponse(tracedResponse().addAttribute(HttpMetrics.PathLabel, label))
    registry.responses.value(Seq(PathDimension(label))) shouldBe 1
    registry.responses.value(Seq(PathDimension("unlabelled"))) shouldBe 0
  }

  it should "increment proper custom dimension" in new Fixture(
    TestRegistry.settings.withServerDimensions(List(TestDimension))
  ) {
    registry.onConnection()
    registry.connections.value(Seq(TestDimension)) shouldBe 1

    registry.onRequest(tracedRequest())
    registry.onResponse(tracedResponse())
    registry.requests.value(Seq(TestDimension)) shouldBe 1
    registry.responses.value(Seq(TestDimension)) shouldBe 1
  }
}

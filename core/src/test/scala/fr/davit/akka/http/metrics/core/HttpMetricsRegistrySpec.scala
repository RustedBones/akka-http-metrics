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
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{
  MethodDimension,
  PathDimension,
  StatusGroupDimension,
  TraceTimestamp
}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class HttpMetricsRegistrySpec
    extends TestKit(ActorSystem("HttpMetricsRegistrySpec"))
    with AnyFlatSpecLike
    with Matchers
    with ScalaFutures {

  implicit val materializer: Materializer = Materializer(system)

  object TestDimension extends Dimension {
    override def key: String   = "env"
    override def value: String = "test"
  }

  val testRequest  = HttpRequest().addAttribute(TraceTimestamp, Deadline.now)
  val testResponse = HttpResponse()

  abstract class Fixture(settings: HttpMetricsSettings = TestRegistry.settings) {
    val registry = new TestRegistry(settings)
  }

  "HttpMetricsRegistry" should "set the request timestamp" in new Fixture() {
    registry.onRequest(HttpRequest()).attribute(TraceTimestamp) shouldBe defined
  }

  it should "compute the number of requests" in new Fixture() {
    registry.requests.value() shouldBe 0
    registry.onRequest(testRequest)
    registry.requests.value() shouldBe 1
    registry.onRequest(testRequest)
    registry.requests.value() shouldBe 2
  }

  it should "compute the number of failures" in new Fixture() {
    registry.requestsFailures.value() shouldBe 0
    registry.onFailure(testRequest, new Exception("BOOM!"))
    registry.requestsFailures.value() shouldBe 1
  }

  it should "compute the number of active requests" in new Fixture() {
    registry.requestsActive.value() shouldBe 0
    registry.onRequest(testRequest)
    registry.onRequest(testRequest)
    registry.requestsActive.value() shouldBe 2
    registry.onResponse(testRequest, testResponse)
    registry.onFailure(testRequest, new Exception("BOOM!"))
    registry.requestsActive.value() shouldBe 0
  }

  it should "compute the requests size" in new Fixture() {
    val data    = "This is the request content"
    val request = HttpRequest(entity = data)
    registry.requestsSize.values() shouldBe empty
    registry.onRequest(request).discardEntityBytes().future().futureValue
    registry.requestsSize.values().head shouldBe data.getBytes.length
  }

  it should "compute the requests size for streamed data" in new Fixture() {
    val data    = Source(List("a", "b", "c")).map(ByteString.apply)
    val request = testRequest.withEntity(HttpEntity(ContentTypes.`application/octet-stream`, data))
    registry.requestsSize.values() shouldBe empty
    registry.onRequest(request).discardEntityBytes().future().futureValue
    registry.requestsSize.values().head shouldBe "abc".getBytes.length
  }

  it should "not transform strict requests" in new Fixture() {
    registry.onRequest(testRequest).entity.isKnownEmpty() shouldBe true
    registry.onRequest(testRequest.withEntity("strict data")).entity.isStrict() shouldBe true
  }

  it should "compute the number of errors" in new Fixture() {
    registry.responsesErrors.value() shouldBe 0
    registry.onResponse(testRequest, testResponse.withStatus(StatusCodes.OK))
    registry.onResponse(testRequest, testResponse.withStatus(StatusCodes.TemporaryRedirect))
    registry.onResponse(testRequest, testResponse.withStatus(StatusCodes.BadRequest))
    registry.responsesErrors.value() shouldBe 0
    registry.onResponse(testRequest, testResponse.withStatus(StatusCodes.InternalServerError))
    registry.responsesErrors.value() shouldBe 1
  }

  it should "compute the response size" in new Fixture() {
    val data     = "This is the response content"
    val request  = testRequest
    val response = HttpResponse(entity = data)
    registry.responsesSize.values() shouldBe empty
    registry.onResponse(request, response).discardEntityBytes().future().futureValue
    registry.responsesSize.values().head shouldBe data.getBytes.length
  }

  it should "compute the response size for streamed data" in new Fixture() {
    val data     = Source(List("a", "b", "c")).map(ByteString.apply)
    val response = testResponse.withEntity(HttpEntity(ContentTypes.`application/octet-stream`, data))
    registry.responsesSize.values() shouldBe empty
    registry.onResponse(testRequest, response).discardEntityBytes().future().futureValue
    registry.responsesSize.values().head shouldBe "abc".getBytes.length
  }

  it should "compute the response time" in new Fixture() {
    val duration = 500.millis
    val start    = Deadline.now - duration
    val request  = testRequest.addAttribute(TraceTimestamp, start)
    registry.responsesDuration.values() shouldBe empty
    registry.onResponse(request, testResponse).discardEntityBytes().future().futureValue
    registry.responsesDuration.values().head should be > duration
  }

  it should "not transform strict responses" in new Fixture() {
    registry.onResponse(testRequest, testResponse).entity.isKnownEmpty() shouldBe true
    registry.onResponse(testRequest, testResponse.withEntity("strict data")).entity.isStrict() shouldBe true
  }

  it should "not transform default responses" in new Fixture() {
    val data   = Source(List("a", "b", "c")).map(ByteString.apply)
    val length = "abc".getBytes.length.toLong

    val response = testResponse
      .withProtocol(HttpProtocols.`HTTP/1.0`) // HTTP/1.0 does not support Chucking. stream MUST not be transformed
      .withEntity(HttpEntity.Default(ContentTypes.`application/octet-stream`, length, data))

    registry.onResponse(testRequest, response).entity shouldBe a[HttpEntity.Default]
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

  it should "add method dimension when enabled" in new Fixture(
    TestRegistry.settings.withIncludeMethodDimension(true)
  ) {
    registry.onRequest(testRequest)
    registry.onResponse(testRequest, testResponse)
    registry.onFailure(testRequest, new Exception("BOOM!"))
    registry.requests.value(Seq(MethodDimension(HttpMethods.GET))) shouldBe 1
    registry.requests.value(Seq(MethodDimension(HttpMethods.PUT))) shouldBe 0
    registry.requestsFailures.value(Seq(MethodDimension(HttpMethods.GET))) shouldBe 1
    registry.requestsFailures.value(Seq(MethodDimension(HttpMethods.PUT))) shouldBe 0
    registry.responses.value(Seq(MethodDimension(HttpMethods.GET))) shouldBe 1
    registry.responses.value(Seq(MethodDimension(HttpMethods.PUT))) shouldBe 0
  }

  it should "add status code dimension when enabled" in new Fixture(
    TestRegistry.settings.withIncludeStatusDimension(true)
  ) {
    registry.onResponse(testRequest, testResponse)
    registry.responses.value(Seq(StatusGroupDimension(StatusCodes.OK))) shouldBe 1
    registry.responses.value(Seq(StatusGroupDimension(StatusCodes.Found))) shouldBe 0
    registry.responses.value(Seq(StatusGroupDimension(StatusCodes.BadRequest))) shouldBe 0
    registry.responses.value(Seq(StatusGroupDimension(StatusCodes.InternalServerError))) shouldBe 0
  }

  it should "default label dimension to 'unlabelled' when enabled but not annotated by directives" in new Fixture(
    TestRegistry.settings.withIncludePathDimension(true)
  ) {
    registry.onResponse(testRequest, testResponse)
    registry.responses.value(Seq(PathDimension("unlabelled"))) shouldBe 1
    registry.responses.value(Seq(PathDimension("unhandled"))) shouldBe 0
  }

  it should "increment proper label dimension" in new Fixture(
    TestRegistry.settings.withIncludePathDimension(true)
  ) {
    val label = "/api"
    registry.onResponse(testRequest, testResponse.addAttribute(HttpMetrics.PathLabel, label))
    registry.responses.value(Seq(PathDimension(label))) shouldBe 1
    registry.responses.value(Seq(PathDimension("unlabelled"))) shouldBe 0
  }

  it should "increment proper custom dimension" in new Fixture(
    TestRegistry.settings.withServerDimensions(List(TestDimension))
  ) {
    registry.onConnection()
    registry.connections.value(Seq(TestDimension)) shouldBe 1

    registry.onRequest(testRequest)
    registry.onResponse(testRequest, testResponse)
    registry.requests.value(Seq(TestDimension)) shouldBe 1
    registry.responses.value(Seq(TestDimension)) shouldBe 1
  }
}

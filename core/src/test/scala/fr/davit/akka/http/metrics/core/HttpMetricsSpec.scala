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
import akka.stream.scaladsl.{Flow, Keep, Tcp}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalamock.matchers.ArgCapture.CaptureOne
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext

class HttpMetricsSpec
    extends TestKit(ActorSystem("HttpMetricsSpec"))
    with AnyFlatSpecLike
    with Matchers
    with ScalaFutures
    with MockFactory
    with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = system.dispatcher

  abstract class BindingFixture {
    val metricsHandler = mock[HttpMetricsHandler]

    val local      = new InetSocketAddress(1234)
    val remote     = new InetSocketAddress("host", 5678)
    val connection = Tcp.IncomingConnection(local, remote, Flow.apply)

    val telemetry = new HttpMetrics.Telemetry(system) {
      override def clientMetrics: HttpMetricsHandler = metricsHandler
      override def serverMetrics: HttpMetricsHandler = metricsHandler
    }

    val (source, sink) = TestSource
      .probe[Tcp.IncomingConnection]
      .via(telemetry.serverBinding)
      .toMat(TestSink.probe[Tcp.IncomingConnection])(Keep.both)
      .run()
  }

  abstract class ConnectionFixture[T] {
    val metricsHandler = mock[HttpMetricsHandler]
    val server         = mockFunction[HttpRequest, HttpResponse]

    val telemetry = new HttpMetrics.Telemetry(system) {
      override def clientMetrics: HttpMetricsHandler = metricsHandler
      override def serverMetrics: HttpMetricsHandler = metricsHandler
    }

    val (source, sink) = TestSource
      .probe[HttpRequest]
      .via(telemetry.serverConnection.reversed.join(Flow.fromFunction(server)))
      .toMat(TestSink.probe[HttpResponse])(Keep.both)
      .run()
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "HttpMetrics" should "call the metrics handler on connection" in new BindingFixture {
    sink.request(1)
    source.sendNext(connection)

    // propagated connection should have been modified
    val propagated = sink.expectNext()
    propagated should not be connection

    // handler is called only when connection materializes
    (metricsHandler.onConnection _)
      .expects()
      .returns(())

    val (in, out) = TestSource
      .probe[ByteString]
      .via(propagated.flow)
      .toMat(TestSink.probe[ByteString])(Keep.both)
      .run()
    out.request(1)

    (metricsHandler.onDisconnection _)
      .expects()
      .returns(())

    // close connection
    in.sendComplete()
    out.expectComplete()

    source.sendComplete()
    sink.expectComplete()
  }

  it should "call the metrics handler on handled requests" in new ConnectionFixture {
    val request  = HttpRequest()
    val response = HttpResponse()

    val actualRequest  = CaptureOne[HttpRequest]()
    val actualResponse = CaptureOne[HttpResponse]()

    (metricsHandler.onRequest _)
      .expects(capture(actualRequest))
      .onCall { (req: HttpRequest) => req }

    server
      .expects(*)
      .returns(response)

    (metricsHandler.onResponse _)
      .expects(capture(actualResponse))
      .onCall { (resp: HttpResponse) => resp }

    sink.request(1)
    source.sendNext(request)
    sink.expectNext()

    source.sendComplete()
    sink.expectComplete()

    actualRequest.value shouldBe request
    actualResponse.value shouldBe response
  }
}

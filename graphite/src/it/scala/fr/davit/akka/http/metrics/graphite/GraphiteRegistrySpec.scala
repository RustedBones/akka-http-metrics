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

package fr.davit.akka.http.metrics.graphite

import java.net.InetSocketAddress
import java.time.{Clock, Instant, ZoneId}

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.io.{IO, Tcp}
import akka.testkit.{TestActor, TestKit, TestProbe}
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{PathDimension, StatusGroupDimension}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class GraphiteRegistrySpec
    extends TestKit(ActorSystem("GraphiteRegistrySpec"))
    with AnyFlatSpecLike
    with Matchers
    with BeforeAndAfterAll {

  val dimensions = Seq(PathDimension("/api"), StatusGroupDimension(StatusCodes.OK))
  val timestamp = Instant.ofEpochSecond(1234)

  def withFixture(test: (TestProbe, GraphiteRegistry) => Any) = {
    val carbon  = TestProbe()
    val handler = TestProbe()
    carbon.send(IO(Tcp), Tcp.Bind(carbon.ref, new InetSocketAddress(0)))
    val port   = carbon.expectMsgType[Tcp.Bound].localAddress.getPort
    val socket = carbon.sender()
    carbon.setAutoPilot(new TestActor.AutoPilot {
      override def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = msg match {
        case _: Tcp.Connected =>
          sender ! Tcp.Register(handler.ref)
          TestActor.KeepRunning
      }
    })

    val client   = new CarbonClient("localhost", port) {
      override val clock: Clock = Clock.fixed(timestamp, ZoneId.systemDefault())
    }
    val registry = GraphiteRegistry(client)
    try {
      test(handler, registry)
    } finally {
//      client.close()
      socket ! Tcp.Unbind
    }
  }

  override def afterAll(): Unit = {
    shutdown()
    super.afterAll()
  }

  "GraphiteRegistry" should "send active datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.active.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.requests.active 1 1234\n"
  }

  it should "send requests datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.requests.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.requests 1 1234\n"
  }

  it should "send receivedBytes datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.receivedBytes.update(3)
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.requests.bytes 3 1234\n"

    registry.receivedBytes.update(3, dimensions)
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.requests.bytes;path=/api;status=2xx 3 1234\n"
  }

  it should "send responses datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.responses.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.responses 1 1234\n"

    registry.responses.inc(dimensions)
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.responses;path=/api;status=2xx 1 1234\n"
  }

  it should "send errors datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.errors.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.responses.errors 1 1234\n"

    registry.errors.inc(dimensions)
    carbon
      .expectMsgType[Tcp.Received]
      .data
      .utf8String shouldBe "akka.http.responses.errors;path=/api;status=2xx 1 1234\n"
  }

  it should "send duration datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.duration.observe(3.seconds)
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.responses.duration 3000 1234\n"

    registry.duration.observe(3.seconds, dimensions)
    carbon
      .expectMsgType[Tcp.Received]
      .data
      .utf8String shouldBe "akka.http.responses.duration;path=/api;status=2xx 3000 1234\n"
  }

  it should "send sentBytes datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.sentBytes.update(3)
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.responses.bytes 3 1234\n"

    registry.sentBytes.update(3, dimensions)
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.responses.bytes;path=/api;status=2xx 3 1234\n"
  }

  it should "send connected datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.connected.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.connections.active 1 1234\n"
  }
  it should "send connections datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.connections.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.connections 1 1234\n"
  }

}

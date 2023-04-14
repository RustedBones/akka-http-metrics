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

import org.apache.pekko.actor.{ActorRef, ActorSystem}
import org.apache.pekko.io.{IO, Tcp}
import org.apache.pekko.testkit.{TestActor, TestKit, TestProbe}
import fr.davit.akka.http.metrics.core.{Dimension, PathLabeler, StatusGroupLabeler}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.net.InetSocketAddress
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.duration._

class GraphiteRegistrySpec
    extends TestKit(ActorSystem("GraphiteRegistrySpec"))
    with AnyFlatSpecLike
    with Matchers
    with BeforeAndAfterAll {

  val dimensions = Seq(Dimension(PathLabeler.name, "/api"), Dimension(StatusGroupLabeler.name, "2xx"))
  val timestamp  = Instant.ofEpochSecond(1234)

  def withFixture(test: (TestProbe, GraphiteRegistry) => Any) = {
    val carbon  = TestProbe()
    val handler = TestProbe()
    carbon.send(IO(Tcp), Tcp.Bind(carbon.ref, new InetSocketAddress(0)))
    val port   = carbon.expectMsgType[Tcp.Bound].localAddress.getPort
    val socket = carbon.sender()
    carbon.setAutoPilot((sender: ActorRef, msg: Any) =>
      msg match {
        case _: Tcp.Connected =>
          sender ! Tcp.Register(handler.ref)
          TestActor.KeepRunning
        case _ =>
          throw new Exception(s"Unexpected message $msg")
      }
    )

    val client = new CarbonClient("localhost", port) {
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

  "GraphiteRegistry" should "send requestsActive datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.requestsActive.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.requests.active 1 1234\n"
  }

  it should "send requests datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.requests.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.requests 1 1234\n"
  }

  it should "send requestsSize datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.requestsSize.update(3)
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.requests.bytes 3 1234\n"

    registry.requestsSize.update(3, dimensions)
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.requests.bytes;path=/api;status=2xx 3 1234\n"
  }

  it should "send responses datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.responses.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.responses 1 1234\n"

    registry.responses.inc(dimensions)
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.responses;path=/api;status=2xx 1 1234\n"
  }

  it should "send responsesErrors datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.responsesErrors.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.responses.errors 1 1234\n"

    registry.responsesErrors.inc(dimensions)
    carbon
      .expectMsgType[Tcp.Received]
      .data
      .utf8String shouldBe "akka.http.responses.errors;path=/api;status=2xx 1 1234\n"
  }

  it should "send responsesDuration datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.responsesDuration.observe(3.seconds)
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.responses.duration 3000 1234\n"

    registry.responsesDuration.observe(3.seconds, dimensions)
    carbon
      .expectMsgType[Tcp.Received]
      .data
      .utf8String shouldBe "akka.http.responses.duration;path=/api;status=2xx 3000 1234\n"
  }

  it should "send responsesSize datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.responsesSize.update(3)
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.responses.bytes 3 1234\n"

    registry.responsesSize.update(3, dimensions)
    carbon
      .expectMsgType[Tcp.Received]
      .data
      .utf8String shouldBe "akka.http.responses.bytes;path=/api;status=2xx 3 1234\n"
  }

  it should "send connectionsActive datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.connectionsActive.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.connections.active 1 1234\n"
  }
  it should "send connections datagrams to the carbon server" in withFixture { (carbon, registry) =>
    registry.connections.inc()
    carbon.expectMsgType[Tcp.Received].data.utf8String shouldBe "akka.http.connections 1 1234\n"
  }

}

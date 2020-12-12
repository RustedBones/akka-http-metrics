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
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.testkit.TestKit
import fr.davit.akka.http.metrics.core.HttpMetrics._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

class HttpMetricsItSpec
    extends TestKit(ActorSystem("HttpMetricsItSpec"))
    with AnyFlatSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  implicit val defaultPatience = PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  override def afterAll(): Unit = {
    Http().shutdownAllConnectionPools()
    TestKit.shutdownActorSystem(system)
  }

  trait Fixture {
    val settings: HttpMetricsSettings = TestRegistry.settings
      .withNamespace("com.example.service")

    val registry = new TestRegistry(settings)

    val route: Route =
      get {
        complete("Hello world")
      }
  }

  "HttpMetrics" should "record metrics on flow handler" in new Fixture {
    val binding = Http()
      .newMeteredServerAt("localhost", 0, registry)
      .bindFlow(route)
      .futureValue

    val uri     = Uri()
      .withScheme("http")
      .withAuthority(binding.localAddress.getHostString, binding.localAddress.getPort)
    val request = HttpRequest().withUri(uri)

    val response = Http()
      .singleRequest(request)
      .futureValue

    response.status shouldBe StatusCodes.OK
    registry.connections.value() shouldBe 1
    registry.requests.value() shouldBe 1

    response.discardEntityBytes()
    binding.terminate(30.seconds).futureValue
  }

  it should "record metrics on function handler" in new Fixture {
    val binding = Http()
      .newMeteredServerAt("localhost", 0, registry)
      .bind(route)
      .futureValue

    val uri     = Uri()
      .withScheme("http")
      .withAuthority(binding.localAddress.getHostString, binding.localAddress.getPort)
    val request = HttpRequest().withUri(uri)
    val response = Http()
      .singleRequest(request)
      .futureValue

    response.status shouldBe StatusCodes.OK
    registry.connections.value() shouldBe 0 // No connection metrics with function handler
    registry.requests.value() shouldBe 1

    response.discardEntityBytes()
    binding.terminate(30.seconds).futureValue
  }
}

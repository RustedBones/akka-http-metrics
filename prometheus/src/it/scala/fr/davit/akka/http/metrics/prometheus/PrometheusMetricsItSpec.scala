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

package fr.davit.akka.http.metrics.prometheus

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.testkit.TestKit
import fr.davit.akka.http.metrics.core.HttpMetrics._
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers._
import io.prometheus.client.hotspot.DefaultExports
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

class PrometheusMetricsItSpec
    extends TestKit(ActorSystem("PrometheusMetricsItSpec"))
    with AnyFlatSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {

  implicit val defaultPatience = PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  override def afterAll(): Unit = {
    Http().shutdownAllConnectionPools()
    TestKit.shutdownActorSystem(system)
  }

  "PrometheusMetrics" should "expose external metrics" in {
    val settings = PrometheusSettings.default
      .withIncludeMethodDimension(true)
      .withIncludePathDimension(true)
      .withIncludeStatusDimension(true)
    DefaultExports.initialize() // JVM
    val registry = PrometheusRegistry(settings = settings)

    val route: Route = (get & path("metrics"))(metrics(registry))

    val binding = Http()
      .newMeteredServerAt("localhost", 0, registry)
      .bindFlow(route)
      .futureValue

    val uri = Uri("/metrics")
      .withScheme("http")
      .withAuthority(binding.localAddress.getHostString, binding.localAddress.getPort)
    val request = HttpRequest().withUri(uri)

    val response = Http()
      .singleRequest(request)
      .futureValue

    response.status shouldBe StatusCodes.OK
    val body = Unmarshal(response).to[String].futureValue

    body
      .split('\n')
      .filter(_.startsWith("# TYPE ")) should contain allElementsOf Seq(
      "# TYPE akka_http_connections_active gauge",
      "# TYPE akka_http_connections_created gauge",
      "# TYPE akka_http_connections_total counter",
      "# TYPE akka_http_requests_active gauge",
      "# TYPE akka_http_requests_created gauge",
      "# TYPE akka_http_requests_size_bytes histogram",
      "# TYPE akka_http_requests_size_bytes_created gauge",
      "# TYPE akka_http_requests_total counter",
      "# TYPE jvm_buffer_pool_capacity_bytes gauge",
      "# TYPE jvm_buffer_pool_used_buffers gauge",
      "# TYPE jvm_buffer_pool_used_bytes gauge",
      "# TYPE jvm_classes_currently_loaded gauge",
      "# TYPE jvm_classes_loaded_total counter",
      "# TYPE jvm_classes_unloaded_total counter",
      "# TYPE jvm_gc_collection_seconds summary",
      "# TYPE jvm_info gauge",
      "# TYPE jvm_memory_bytes_committed gauge",
      "# TYPE jvm_memory_bytes_init gauge",
      "# TYPE jvm_memory_bytes_max gauge",
      "# TYPE jvm_memory_bytes_used gauge",
      "# TYPE jvm_memory_objects_pending_finalization gauge",
      "# TYPE jvm_memory_pool_allocated_bytes_created gauge",
      "# TYPE jvm_memory_pool_allocated_bytes_total counter",
      "# TYPE jvm_memory_pool_bytes_committed gauge",
      "# TYPE jvm_memory_pool_bytes_init gauge",
      "# TYPE jvm_memory_pool_bytes_max gauge",
      "# TYPE jvm_memory_pool_bytes_used gauge",
      "# TYPE jvm_memory_pool_collection_committed_bytes gauge",
      "# TYPE jvm_memory_pool_collection_init_bytes gauge",
      "# TYPE jvm_memory_pool_collection_max_bytes gauge",
      "# TYPE jvm_memory_pool_collection_used_bytes gauge",
      "# TYPE jvm_threads_current gauge",
      "# TYPE jvm_threads_daemon gauge",
      "# TYPE jvm_threads_deadlocked gauge",
      "# TYPE jvm_threads_deadlocked_monitor gauge",
      "# TYPE jvm_threads_peak gauge",
      "# TYPE jvm_threads_started_total counter",
      "# TYPE jvm_threads_state gauge",
      "# TYPE process_cpu_seconds_total counter",
      "# TYPE process_max_fds gauge",
      "# TYPE process_open_fds gauge",
      "# TYPE process_resident_memory_bytes gauge",
      "# TYPE process_start_time_seconds gauge",
      "# TYPE process_virtual_memory_bytes gauge"
    )

    binding.terminate(30.seconds).futureValue
    Http()
  }
}

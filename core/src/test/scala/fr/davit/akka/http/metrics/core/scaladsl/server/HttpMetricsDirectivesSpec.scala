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

import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import fr.davit.akka.http.metrics.core.{HttpMetrics, TestRegistry}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HttpMetricsDirectivesSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  import HttpMetricsDirectives._

  "HttpMetricsDirectives" should "expose the registry" in {
    implicit val marshaller = StringMarshaller.compose[TestRegistry](r => s"active: ${r.requestsActive.value()}")
    val registry            = new TestRegistry()
    registry.requestsActive.inc()

    val route = path("metrics") {
      metrics(registry)
    }

    Get("/metrics") ~> route ~> check {
      responseAs[String] shouldBe "active: 1"
    }
  }

  it should "put ignored attribute" in {
    val route = ignoreMetrics {
      path("private") {
        attribute(HttpMetrics.Ignored) { _ =>
          complete(StatusCodes.OK)
        }
      }
    }

    Get("/private") ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  it should "put label on path" in {
    val route = pathPrefixLabeled("api") {
      pathPrefix("user" / LongNumber) { _ =>
        path("address") {
          complete(StatusCodes.OK)
        }
      }
    }

    Get("/api/user/1234/address") ~> route ~> check {
      response.attribute(HttpMetrics.PathLabel) shouldBe Some("/api")
    }
  }

  it should "combine labelled segments" in {
    val route = pathPrefixLabeled("api") {
      pathPrefixLabeled("user" / LongNumber, "user/:userId") { _ =>
        pathLabeled("address") {
          complete(StatusCodes.OK)
        }
      }
    }

    Get("/api/user/1234/address") ~> route ~> check {
      response.attribute(HttpMetrics.PathLabel) shouldBe Some("/api/user/:userId/address")
    }
  }

  it should "not add extra attribute when label directives are not used" in {
    val route = pathPrefix("api") {
      pathPrefix("user" / LongNumber) { _ =>
        path("address") {
          complete(StatusCodes.OK)
        }
      }
    }

    Get("/api/user/1234/address") ~> route ~> check {
      response.attribute(HttpMetrics.PathLabel) shouldBe empty
    }
  }
}

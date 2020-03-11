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
import akka.http.scaladsl.server.PathMatchers.IntNumber
import akka.http.scaladsl.testkit.ScalatestRouteTest
import fr.davit.akka.http.metrics.core.TestRegistry
import fr.davit.akka.http.metrics.core.scaladsl.model.{PathLabelHeader, SubPathLabelHeader}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HttpMetricsDirectivesSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  import HttpMetricsDirectives._

  "HttpMetricsDirectives" should "expose the registry" in {
    implicit val marshaller = StringMarshaller.compose[TestRegistry](r => s"active: ${r.active.value()}")
    val registry            = new TestRegistry()
    registry.active.inc()

    val route = path("metrics") {
      metrics(registry)
    }

    Get("/metrics") ~> route ~> check {
      responseAs[String] shouldBe "active: 1"
    }
  }

  it should "label segment in headers" in {
    val route = pathPrefix("api") {
      pathPrefixLabeled("user" / LongNumber, "user/:userId") { _ =>
        path("address") {
          complete(StatusCodes.OK)
        }
      }
    }

    Get("/api/user/1234/address") ~> route ~> check {
      header[PathLabelHeader] shouldBe Some(SubPathLabelHeader("/user/1234/address", "/user/:userId/address"))
    }
  }

  it should "compile labelled segments" in {
    val route =
      pathPrefix("api") {
        pathPrefixLabeled("user" / LongNumber, "user/:userId") { _ =>
          pathLabeled("address" / IntNumber, "address/:addressId") { _ =>
            complete(StatusCodes.OK)
          }
        }
      }

    Get("/api/user/1234/address/1") ~> route ~> check {
      header[PathLabelHeader] shouldBe Some(
        SubPathLabelHeader("/user/1234/address/1", "/user/:userId/address/:addressId")
      )
    }
  }

}

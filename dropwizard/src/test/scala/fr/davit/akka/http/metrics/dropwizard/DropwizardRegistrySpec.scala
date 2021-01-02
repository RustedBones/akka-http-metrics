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

package fr.davit.akka.http.metrics.dropwizard

import akka.http.scaladsl.model.StatusCodes
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{PathDimension, StatusGroupDimension}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class DropwizardRegistrySpec extends AnyFlatSpec with Matchers {

  val dimensions = Seq(StatusGroupDimension(StatusCodes.OK), PathDimension("/api"))

  trait Fixture {
    val registry = DropwizardRegistry()

    def underlyingCounter(name: String): Long = {
      registry.underlying.getCounters.asScala(name).getCount
    }

    def underlyingHistogram(name: String): Long = {
      registry.underlying.getHistograms.asScala(name).getSnapshot.getValues.sum
    }

    def underlyingTimer(name: String): Long = {
      registry.underlying.getTimers.asScala(name).getSnapshot.getValues.sum
    }

  }

  "DropwizardRegistry" should "set requestsActive metrics in the underlying registry" in new Fixture {
    registry.requestsActive.inc()
    underlyingCounter("akka.http.requests.active") shouldBe 1L
  }

  it should "set requests metrics in the underlying registry" in new Fixture {
    registry.requests.inc()
    underlyingCounter("akka.http.requests") shouldBe 1L
  }

  it should "set requestsSize metrics in the underlying registry" in new Fixture {
    registry.requestsSize.update(3)
    underlyingHistogram("akka.http.requests.bytes") shouldBe 3L
  }

  it should "set responses metrics in the underlying registry" in new Fixture {
    registry.responses.inc()
    underlyingCounter("akka.http.responses") shouldBe 1L

    registry.responses.inc(dimensions)
    underlyingCounter("akka.http.responses") shouldBe 2L
  }

  it should "set responsesErrors metrics in the underlying registry" in new Fixture {
    registry.responsesErrors.inc()
    underlyingCounter("akka.http.responses.errors") shouldBe 1L

    registry.responsesErrors.inc(dimensions)
    underlyingCounter("akka.http.responses.errors") shouldBe 2L
  }

  it should "set responsesDuration metrics in the underlying registry" in new Fixture {
    registry.responsesDuration.observe(3.seconds)
    underlyingTimer("akka.http.responses.duration") shouldBe 3000000000L

    registry.responsesDuration.observe(3.seconds, dimensions)
    underlyingTimer("akka.http.responses.duration") shouldBe 6000000000L
  }

  it should "set responsesSize metrics in the underlying registry" in new Fixture {
    registry.responsesSize.update(3)
    underlyingHistogram("akka.http.responses.bytes") shouldBe 3L

    registry.responsesSize.update(3, dimensions)
    underlyingHistogram("akka.http.responses.bytes") shouldBe 6L
  }

  it should "set connectionsActive metrics in the underlying registry" in new Fixture {
    registry.connectionsActive.inc()
    underlyingCounter("akka.http.connections.active") shouldBe 1L
  }

  it should "set connections metrics in the underlying registry" in new Fixture {
    registry.connections.inc()
    underlyingCounter("akka.http.connections") shouldBe 1L
  }
}

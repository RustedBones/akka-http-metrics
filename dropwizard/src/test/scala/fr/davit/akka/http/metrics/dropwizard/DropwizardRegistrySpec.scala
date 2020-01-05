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
import fr.davit.akka.http.metrics.core.Dimension
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{PathDimension, StatusGroupDimension}
import io.dropwizard.metrics5.{Counter, MetricName, MetricRegistry}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

import scala.collection.JavaConverters._

class DropwizardRegistrySpec extends FlatSpec with Matchers {

  val dimensions = Seq(StatusGroupDimension(StatusCodes.OK), PathDimension("/api"))

  trait Fixture {
    val registry = DropwizardRegistry()

    def underlyingCounter(name: String, dims: Seq[Dimension] = Seq.empty): Long = {
      registry.underlying.getCounters.asScala(metricName(name, dims)).getCount
    }

    def underlyingHistogram(name: String, dims: Seq[Dimension] = Seq.empty): Long = {
      registry.underlying.getHistograms.asScala(metricName(name, dims)).getSum
    }

    def underlyingTimer(name: String, dims: Seq[Dimension] = Seq.empty): Long = {
      registry.underlying.getTimers.asScala(metricName(name, dims)).getSum
    }

    private def metricName(name: String, dims: Seq[Dimension]): MetricName = {
      MetricName.build(name).tagged(dims.map(d => d.key -> d.value).toMap.asJava)
    }
  }

  "DropwizardRegistry" should "set active metrics in the underlying registry" in new Fixture {
    registry.active.inc()
    underlyingCounter("akka.http.requests.active") shouldBe 1L
  }

  it  should "set requests metrics in the underlying registry" in new Fixture {
    registry.requests.inc()
    underlyingCounter("akka.http.requests") shouldBe 1L
  }

  it  should "set receivedBytes metrics in the underlying registry" in new Fixture {
    registry.receivedBytes.update(3)
    underlyingHistogram("akka.http.requests.bytes") shouldBe 3L
  }

  it  should "set responses metrics in the underlying registry" in new Fixture {
    registry.responses.inc()
    underlyingCounter("akka.http.responses") shouldBe 1L

    registry.responses.inc(dimensions)
    underlyingCounter("akka.http.responses", dimensions) shouldBe 1L
  }

  it  should "set errors metrics in the underlying registry" in new Fixture {
    registry.errors.inc()
    underlyingCounter("akka.http.responses.errors") shouldBe 1L

    registry.errors.inc(dimensions)
    underlyingCounter("akka.http.responses.errors", dimensions) shouldBe 1L
  }

  it  should "set duration metrics in the underlying registry" in new Fixture {
    registry.duration.observe(3.seconds)
    underlyingTimer("akka.http.responses.duration") shouldBe 3000000000L

    registry.duration.observe(3.seconds, dimensions)
    underlyingTimer("akka.http.responses.duration", dimensions) shouldBe 3000000000L
  }

  it  should "set sentBytes metrics in the underlying registry" in new Fixture {
    registry.sentBytes.update(3)
    underlyingHistogram("akka.http.responses.bytes") shouldBe 3L

    registry.sentBytes.update(3, dimensions)
    underlyingHistogram("akka.http.responses.bytes", dimensions) shouldBe 3L
  }

  it  should "set connected metrics in the underlying registry" in new Fixture {
    registry.connected.inc()
    underlyingCounter("akka.http.connections.active") shouldBe 1L
  }

  it  should "set connections metrics in the underlying registry" in new Fixture {
    registry.connections.inc()
    underlyingCounter("akka.http.connections") shouldBe 1L
  }
}

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

import java.util.concurrent.atomic.LongAdder

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.HttpEntity

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object TestRegistry {
  implicit val marshaller: ToEntityMarshaller[TestRegistry] = Marshaller.opaque(_ => HttpEntity.Empty)

  private def keyer(dimensions: Seq[Dimension]): String = dimensions.mkString(":")

  class TestCounter extends Counter[Long] {
    protected val acc = mutable.Map[String, Long]()

    override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
      val key = keyer(dimensions)
      acc.get(key) match {
        case Some(v) => acc += (key -> (v + 1))
        case None    => acc += (key -> 1)
      }
    }

    def value(dimensions: Seq[Dimension] = Seq.empty): Long = acc.getOrElse(keyer(dimensions), 0)
  }

  class TestGauge extends TestCounter with Gauge[Long] {
    override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
      val key = keyer(dimensions)
      acc.get(key) match {
        case Some(v) => acc += (key -> (v - 1))
        case None    => acc += (key -> -1)
      }
    }
  }

  class TestTimer extends Timer {
    protected val acc = mutable.Map[String, List[FiniteDuration]]()

    override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
      val key = keyer(dimensions)
      acc.get(key) match {
        case Some(vs) => acc += (key -> (duration :: vs))
        case None     => acc += (key -> (duration :: Nil))
      }
    }

    def values(dimensions: Seq[Dimension] = Seq.empty): List[FiniteDuration] = acc.getOrElse(keyer(dimensions), Nil)
  }

  final class TestHistogram extends Histogram[Long] {
    protected val acc = mutable.Map[String, List[Long]]()

    override def update(value: Long, dimensions: Seq[Dimension] = Seq.empty): Unit = {
      val key = keyer(dimensions)
      acc.get(key) match {
        case Some(vs) => acc += (key -> (value :: vs))
        case None     => acc += (key -> (value :: Nil))
      }
    }

    def values(dimensions: Seq[Dimension] = Seq.empty): List[Long] = acc.getOrElse(keyer(dimensions), Nil)
  }

}

final class TestRegistry extends HttpMetricsRegistry {

  import TestRegistry._

  override val active = new TestGauge

  override val requests = new TestCounter

  override val receivedBytes = new TestHistogram

  override val responses = new TestCounter

  override val errors = new TestCounter

  override val duration = new TestTimer

  override val sentBytes = new TestHistogram

  override val connected = new TestGauge

  override val connections = new TestCounter
}

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

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import fr.davit.akka.http.metrics.core.HttpMetricsSettings
import fr.davit.akka.http.metrics.prometheus.Quantiles.Quantile

import scala.concurrent.duration._

sealed trait HistogramConfig

sealed trait TimerConfig

final case class Quantiles(qs: List[Quantile], maxAge: FiniteDuration = 10.minutes, ageBuckets: Int = 5)
    extends HistogramConfig
    with TimerConfig

object Quantiles {

  final case class Quantile(percentile: Double, error: Double = 0.001)

  def apply(percentiles: Double*): Quantiles = {
    val quantiles = percentiles.map { p =>
      // the higher the percentile, the lowe the error
      val error = (1 - p) / 10
      Quantile(p, error)
    }
    Quantiles(quantiles.toList)
  }
}

final case class Buckets(bs: List[Double]) extends HistogramConfig with TimerConfig

object Buckets {
  def apply(b: Double*): Buckets = Buckets(b.toList)
}

final case class PrometheusSettings(
    namespace: String,
    defineError: HttpResponse => Boolean,
    includeStatusDimension: Boolean,
    includePathDimension: Boolean,
    receivedBytesConfig: HistogramConfig,
    durationConfig: TimerConfig,
    sentBytesConfig: HistogramConfig
) extends HttpMetricsSettings {

  override def withNamespace(namespace: String): PrometheusSettings =
    copy(namespace = namespace)

  override def withDefineError(fn: HttpResponse => Boolean): PrometheusSettings =
    copy(defineError = defineError)

  override def withIncludeStatusDimension(include: Boolean): PrometheusSettings =
    copy(includeStatusDimension = include)

  override def withIncludePathDimension(include: Boolean): PrometheusSettings =
    copy(includePathDimension = include)

  def withReceivedBytesConfig(config: HistogramConfig): PrometheusSettings =
    copy(receivedBytesConfig = config)

  def withDurationConfig(config: TimerConfig): PrometheusSettings =
    copy(durationConfig = config)

  def withSentBytesConfig(config: HistogramConfig): PrometheusSettings =
    copy(sentBytesConfig = config)
}

object PrometheusSettings {

  // generic durations adapted to network durations in seconds
  val DurationBuckets: Buckets = {
    Buckets(0.005, 0.01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10)
  }

  // generic buckets adapted to network messages sized
  val BytesBuckets: Buckets = {
    val buckets = Range(0, 1000, 100) ++ Range(1000, 10000, 1000) ++ Range(10000, 100000, 10000)
    Buckets(buckets.map(_.toDouble).toList)
  }

  // basic quantiles
  val DefaultQuantiles: Quantiles = Quantiles(0.75, 0.95, 0.98, 0.99, 0.999)

  val default: PrometheusSettings = PrometheusSettings(
    namespace = "akka_http",
    defineError = _.status.isInstanceOf[StatusCodes.ServerError],
    includeStatusDimension = false,
    includePathDimension = false,
    receivedBytesConfig = BytesBuckets,
    durationConfig = DurationBuckets,
    sentBytesConfig = BytesBuckets
  )

}

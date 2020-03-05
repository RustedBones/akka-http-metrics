package fr.davit.akka.http.metrics.prometheus

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import fr.davit.akka.http.metrics.core.HttpMetricsSettings
import fr.davit.akka.http.metrics.prometheus.Quantiles.Quantile

import scala.concurrent.duration._

sealed trait HistogramSettings

sealed trait TimerSettings

final case class Quantiles(qs: Array[Quantile], maxAge: FiniteDuration = 10.minutes, ageBuckets: Int = 5)
    extends HistogramSettings
    with TimerSettings

object Quantiles {

  final case class Quantile(percentile: Double, error: Double = 0.001)

  def apply(percentiles: Double*): Quantiles = {
    val quantiles = percentiles.map { p =>
      // the higher the percentile, the lowe the error
      val error = (1 - p) / 10
      Quantile(p, error)
    }
    Quantiles(quantiles.toArray)
  }
}

final case class Buckets(bs: Array[Double]) extends HistogramSettings

object Buckets {
  def apply(b: Double*): Buckets = Buckets(b.toArray)
}

final case class PrometheusSettings(
    namespace: String,
    defineError: HttpResponse => Boolean,
    includeStatusDimension: Boolean,
    includePathDimension: Boolean,
    receivedBytesSettings: HistogramSettings,
    durationSettings: TimerSettings,
    sentBytesSettings: HistogramSettings
) extends HttpMetricsSettings {

  override def withNamespace(namespace: String): PrometheusSettings =
    copy(namespace = namespace)

  override def withDefineError(fn: HttpResponse => Boolean): PrometheusSettings =
    copy(defineError = defineError)

  override def withIncludeStatusDimension(include: Boolean): PrometheusSettings =
    copy(includeStatusDimension = include)

  override def withIncludePathDimension(include: Boolean): PrometheusSettings =
    copy(includePathDimension = include)

  def withReceivedBytesSettings(settings: HistogramSettings): PrometheusSettings =
    copy(receivedBytesSettings = settings)

  def withDurationSettings(settings: TimerSettings): PrometheusSettings =
    copy(durationSettings = settings)

  def withSentBytesSettings(settings: HistogramSettings): PrometheusSettings =
    copy(sentBytesSettings = settings)
}

object PrometheusSettings {

  // generic buckets addapted to network packet sized
  private val bytesBuckets = {
    val buckets = Range(100, 1000, 100) ++ Range(1000, 10000, 1000) ++ Range(10000, 100000, 10000)
    Buckets(buckets.map(_.toDouble).toArray)
  }

  // basic quantiles
  private val quantiles = Quantiles(0.75, 0.95, 0.98, 0.99, 0.999)

  val default: PrometheusSettings = PrometheusSettings(
    "akka_http",
    _.status.isInstanceOf[StatusCodes.ServerError],
    includeStatusDimension = false,
    includePathDimension = false,
    bytesBuckets,
    quantiles,
    bytesBuckets
  )

}

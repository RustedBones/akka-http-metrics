package fr.davit.akka.http.metrics.prometheus

trait PrometheusConverters {

  implicit def convertCounter(counter: io.prometheus.client.Counter): PrometheusCounter = new PrometheusCounter(counter)

  implicit def convertGauge(gauge: io.prometheus.client.Gauge): PrometheusGauge = new PrometheusGauge(gauge)

  implicit def convertTimer(summary: io.prometheus.client.Summary): PrometheusTimer = new PrometheusTimer(summary)

  implicit def convertSummary(summary: io.prometheus.client.Summary): PrometheusSummary = new PrometheusSummary(summary)

  implicit def convertHistogram(histogram: io.prometheus.client.Histogram): PrometheusHistogram =
    new PrometheusHistogram(histogram)

}

object PrometheusConverters extends PrometheusConverters

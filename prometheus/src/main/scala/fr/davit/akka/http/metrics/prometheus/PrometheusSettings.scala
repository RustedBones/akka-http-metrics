package fr.davit.akka.http.metrics.prometheus

/**
  * Settings for Prometheus metrics
  *
  * @param durationBuckets     Buckets to use for calculating request duration histogram (seconds).
  * @param requestSizeBuckets  Buckets to use for calculating request size histogram (bytes).
  * @param responseSizeBuckets Buckets to use for calculating response size histogram (bytes).
  */
case class PrometheusSettings(
    durationBuckets: Seq[Double] = Seq(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2, 5, 10),
    requestSizeBuckets: Seq[Double] = Seq(0, 1024, 10240, 102400, 1048576, 10485760),
    responseSizeBuckets: Seq[Double] = Seq(0, 1024, 10240, 102400, 1048576, 10485760))

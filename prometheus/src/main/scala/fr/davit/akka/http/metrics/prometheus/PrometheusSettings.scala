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

/**
  * Settings for Prometheus metrics
  *
  * @param durationBuckets     Buckets to use for calculating request duration histogram (seconds).
  * @param requestSizeBuckets  Buckets to use for calculating request size histogram (bytes).
  * @param responseSizeBuckets Buckets to use for calculating response size histogram (bytes).
  */
case class PrometheusSettings(
    durationBuckets: Seq[Double] = Seq(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10),
    requestSizeBuckets: Seq[Double] = Seq(0, 128, 256, 512, 1024, 10240, 102400, 1048576, 10485760),
    responseSizeBuckets: Seq[Double] = Seq(128, 256, 512, 1024, 10240, 102400, 1048576, 10485760)
)

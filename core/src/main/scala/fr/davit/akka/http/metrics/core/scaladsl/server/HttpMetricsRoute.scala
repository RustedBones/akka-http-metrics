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

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server._
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import fr.davit.akka.http.metrics.core.{HttpMetrics, HttpMetricsHandler}

import scala.concurrent.Future

/**
  * Typeclass to add the metrics capabilities to a route
  *
  */
final class HttpMetricsRoute private (route: Route) extends HttpMetricsDirectives {

  @deprecated("Use Http.newMeteredServerAt(...)...bind() to create metered server bindings.", "1.2.0")
  def recordMetrics(
      metricsHandler: HttpMetricsHandler
  )(implicit materializer: Materializer): Flow[HttpRequest, HttpResponse, NotUsed] = recordMetricsImpl(metricsHandler)

  private[metrics] def recordMetricsImpl(
      metricsHandler: HttpMetricsHandler
  )(implicit materializer: Materializer): Flow[HttpRequest, HttpResponse, NotUsed] =
    HttpMetrics
      .meterFlow(metricsHandler)
      .join(HttpMetrics.metricsRouteToFlow(route)(materializer.system))

  @deprecated("Use Http.newMeteredServerAt(...)...bind() to create metered server bindings.", "1.2.0")
  def recordMetricsAsync(
      metricsHandler: HttpMetricsHandler
  )(implicit materializer: Materializer): HttpRequest => Future[HttpResponse] = {
    val handler = HttpMetrics.metricsRouteToFunction(route)(materializer.system)
    HttpMetrics.meterFunction(handler, metricsHandler)
  }
}

object HttpMetricsRoute {

  implicit def apply(route: Route): HttpMetricsRoute = new HttpMetricsRoute(route)

}

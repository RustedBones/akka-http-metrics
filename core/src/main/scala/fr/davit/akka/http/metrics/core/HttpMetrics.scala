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

import akka.NotUsed
import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Directives, ExceptionHandler, RejectionHandler, Route}
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.{HttpExt, HttpMetricsServerBuilder}
import akka.stream.Materializer
import akka.stream.scaladsl.{BidiFlow, Flow}
import fr.davit.akka.http.metrics.core.scaladsl.model.PathLabelHeader

import scala.concurrent.Future

final class HttpMetrics(private val http: HttpExt) extends AnyVal {

  def newMeteredServerAt(interface: String, port: Int, metricsHandler: HttpMetricsHandler): HttpMetricsServerBuilder =
    HttpMetricsServerBuilder(interface, port, metricsHandler, http.system)

}

object HttpMetrics {

  implicit def enrichHttp(http: HttpExt): HttpMetrics = new HttpMetrics(http)

  private def markUnhandled(inner: Route): Route = {
    Directives.mapResponse(markUnhandled).tapply(_ => inner)
  }

  private def markUnhandled(response: HttpResponse): HttpResponse = {
    response.addHeader(PathLabelHeader.Unhandled)
  }

  /**
    * This will take precedence over the RouteResult.routeToFlow
    * to seal the route with proper handler for metrics labeling
    */
  implicit def metricsRouteToFlow(
      route: Route
  )(implicit system: ClassicActorSystemProvider): Flow[HttpRequest, HttpResponse, NotUsed] =
    Flow[HttpRequest].mapAsync(1)(metricsRouteToFunction(route))

  /**
    * This will take precedence over the RouteResult.routeToFunction
    * to seal the route with proper handler for metrics labeling
    */
  implicit def metricsRouteToFunction(
      route: Route
  )(implicit system: ClassicActorSystemProvider): HttpRequest => Future[HttpResponse] = {
    val routingSettings  = RoutingSettings(system)
    val exceptionHandler = ExceptionHandler.default(routingSettings).andThen(markUnhandled(_))
    val rejectionHandler = RejectionHandler.default.mapRejectionResponse(markUnhandled)

    import akka.http.scaladsl.server.directives.ExecutionDirectives._
    Route.toFunction((handleExceptions(exceptionHandler) & handleRejections(rejectionHandler)).tapply(_ => route))
  }

  def meterFunction(handler: HttpRequest => Future[HttpResponse], metricsHandler: HttpMetricsHandler)(
      implicit materializer: Materializer
  ): HttpRequest => Future[HttpResponse] = { request: HttpRequest =>
    val response = handler(request)
    metricsHandler.onRequest(request, response)
    response
  }

  def meterFlow(
      metricsHandler: HttpMetricsHandler
  ): BidiFlow[HttpRequest, HttpRequest, HttpResponse, HttpResponse, NotUsed] =
    BidiFlow.fromGraph(new MeterStage(metricsHandler))

}

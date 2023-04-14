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

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ClassicActorSystemProvider
import org.apache.pekko.http.scaladsl.HttpExt
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.{Directives, ExceptionHandler, RejectionHandler, Route}
import org.apache.pekko.http.scaladsl.settings.RoutingSettings
import org.apache.pekko.stream.scaladsl.{BidiFlow, Flow}
import fr.davit.akka.http.metrics.core.scaladsl.HttpMetricsServerBuilder

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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
    response.addAttribute(PathLabeler.key, "unhandled")
  }

  /** This will take precedence over the RouteResult.routeToFlow to seal the route with proper handler for metrics
    * labeling
    */
  def metricsRouteToFlow(
      route: Route
  )(implicit system: ClassicActorSystemProvider): Flow[HttpRequest, HttpResponse, NotUsed] =
    Flow[HttpRequest].mapAsync(1)(metricsRouteToFunction(route))

  /** This will take precedence over the RouteResult.routeToFunction to seal the route with proper handler for metrics
    * labeling
    */
  def metricsRouteToFunction(
      route: Route
  )(implicit system: ClassicActorSystemProvider): HttpRequest => Future[HttpResponse] = {
    val routingSettings  = RoutingSettings(system)
    val exceptionHandler = ExceptionHandler.default(routingSettings).andThen(markUnhandled _)
    val rejectionHandler = RejectionHandler.default.mapRejectionResponse(markUnhandled)

    import org.apache.pekko.http.scaladsl.server.directives.ExecutionDirectives._
    Route.toFunction {
      (handleExceptions(exceptionHandler) & handleRejections(rejectionHandler)) {
        route
      }
    }
  }

  def meterFunction(handler: HttpRequest => Future[HttpResponse], metricsHandler: HttpMetricsHandler)(implicit
      executionContext: ExecutionContext
  ): HttpRequest => Future[HttpResponse] =
    (metricsHandler.onRequest _)
      .andThen(r => (r, handler(r)))
      .andThen { case (req, resp) =>
        resp.transform(
          r => metricsHandler.onResponse(req, r),
          e => metricsHandler.onFailure(req, e)
        )
      }

  def meterFunctionSync(
      handler: HttpRequest => HttpResponse,
      metricsHandler: HttpMetricsHandler
  ): HttpRequest => HttpResponse =
    (metricsHandler.onRequest _)
      .andThen(r => (r, Try(handler(r))))
      .andThen {
        case (req, Success(resp)) =>
          metricsHandler.onResponse(req, resp)
        case (req, Failure(e)) =>
          metricsHandler.onFailure(req, e)
          throw e
      }

  def meterFlow(
      metricsHandler: HttpMetricsHandler
  ): BidiFlow[HttpRequest, HttpRequest, HttpResponse, HttpResponse, NotUsed] = BidiFlow
    .fromGraph(new MeterStage(metricsHandler))

}

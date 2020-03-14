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
import akka.http.scaladsl.settings.{ParserSettings, RoutingSettings}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import fr.davit.akka.http.metrics.core.HttpMetricsHandler
import fr.davit.akka.http.metrics.core.scaladsl.model.PathLabelHeader

import scala.concurrent.{ExecutionContextExecutor, Future}

object HttpMetricsRoute {

  implicit def apply(route: Route): HttpMetricsRoute = new HttpMetricsRoute(route)

}

/**
  * Typeclass to add the metrics capabilities to a route
  *
  */
final class HttpMetricsRoute private (route: Route) extends HttpMetricsDirectives {

  private def markUnhandled(inner: Route): Route = {
    Directives.mapResponse(markUnhandled).tapply(_ => inner)
  }

  private def markUnhandled(response: HttpResponse): HttpResponse = {
    response.addHeader(PathLabelHeader.Unhandled)
  }

  def recordMetrics(metricsHandler: HttpMetricsHandler)(
      implicit
      routingSettings: RoutingSettings,
      parserSettings: ParserSettings,
      materializer: Materializer,
      routingLog: RoutingLog,
      executionContext: ExecutionContextExecutor = null,
      rejectionHandler: RejectionHandler = RejectionHandler.default,
      exceptionHandler: ExceptionHandler = null
  ): Flow[HttpRequest, HttpResponse, NotUsed] = {
    // override the execution context passed as parameter, rejection and error handler
    val effectiveEC =
      if (executionContext ne null) executionContext else materializer.executionContext
    val effectiveRejectionHandler = rejectionHandler.mapRejectionResponse(markUnhandled)
    val effectiveExceptionHandler = ExceptionHandler.seal(exceptionHandler).andThen(markUnhandled(_))

    {
      implicit val executionContext: ExecutionContextExecutor = effectiveEC
      implicit val rejectionHandler: RejectionHandler         = effectiveRejectionHandler
      implicit val exceptionHandler: ExceptionHandler         = effectiveExceptionHandler
      Flow[HttpRequest]
        .mapAsync(1)(recordMetricsAsync(metricsHandler))
        .watchTermination() {
          case (mat, completion) =>
            // every connection materializes a stream
            metricsHandler.onConnection(completion)
            mat
        }
    }
  }

  def recordMetricsAsync(metricsHandler: HttpMetricsHandler)(
      implicit
      routingSettings: RoutingSettings,
      parserSettings: ParserSettings,
      materializer: Materializer,
      routingLog: RoutingLog,
      executionContext: ExecutionContextExecutor = null,
      rejectionHandler: RejectionHandler = RejectionHandler.default,
      exceptionHandler: ExceptionHandler = null
  ): HttpRequest => Future[HttpResponse] = { request =>
    val response = Route.asyncHandler(route).apply(request)
    metricsHandler.onRequest(request, response)
    response
  }
}

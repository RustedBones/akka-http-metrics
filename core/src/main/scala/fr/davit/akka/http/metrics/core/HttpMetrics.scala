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
import akka.http.scaladsl.model.{AttributeKey, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directives, ExceptionHandler, RejectionHandler, Route}
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.{HttpExt, HttpMetricsServerBuilder}
import akka.stream.scaladsl.{BidiFlow, Flow}

import java.util.UUID
import scala.concurrent.duration.Deadline
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

final class HttpMetrics(private val http: HttpExt) extends AnyVal {

  def newMeteredServerAt(interface: String, port: Int, metricsHandler: HttpMetricsHandler): HttpMetricsServerBuilder =
    HttpMetricsServerBuilder(interface, port, metricsHandler, http.system)

}

object HttpMetrics {

  val TraceId: AttributeKey[UUID]            = AttributeKey("trace-id", classOf[UUID])
  val TraceTimestamp: AttributeKey[Deadline] = AttributeKey("trace-time", classOf[Deadline])
  val PathLabel: AttributeKey[String]        = AttributeKey("path-label", classOf[String])

  implicit def enrichHttp(http: HttpExt): HttpMetrics = new HttpMetrics(http)

  private def traceRequest(request: HttpRequest): HttpRequest =
    request
      .addAttribute(TraceId, UUID.randomUUID())
      .addAttribute(TraceTimestamp, Deadline.now)

  private def markUnhandled(inner: Route): Route = {
    Directives.mapResponse(markUnhandled).tapply(_ => inner)
  }

  private def markUnhandled(response: HttpResponse): HttpResponse = {
    response.addAttribute(PathLabel, "unhandled")
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
    Route.toFunction {
      // trace the server request to response by extracting the trace attribute
      // and injecting it to the response after being sealed by the rejection and exception handlers
      attribute(TraceId) { id =>
        mapResponse(_.addAttribute(TraceId, id)) {
          (handleExceptions(exceptionHandler) & handleRejections(rejectionHandler)) {
            route
          }
        }
      }
    }
  }

  def meterFunction(handler: HttpRequest => Future[HttpResponse], metricsHandler: HttpMetricsHandler)(
      implicit executionContext: ExecutionContext
  ): HttpRequest => Future[HttpResponse] = { request: HttpRequest =>
    (traceRequest _)
      .andThen(metricsHandler.onRequest)
      .andThen(handler)
      .andThen(
        _.transform(
          metricsHandler.onResponse(request, _),
          metricsHandler.onFailure(request, _)
        )
      )
      .apply(request)
  }

  def meterFunctionSync(
      handler: HttpRequest => HttpResponse,
      metricsHandler: HttpMetricsHandler
  ): HttpRequest => Future[HttpResponse] = { request: HttpRequest =>
    (traceRequest _)
      .andThen(metricsHandler.onRequest)
      .andThen(r => Try(handler(r)))
      .andThen(
        _.transform(
          response => Success(metricsHandler.onResponse(request, response)),
          cause => Failure(metricsHandler.onFailure(request, cause))
        )
      )
      .andThen(Future.fromTry)
      .apply(request)
  }

  def meterFlow(
      metricsHandler: HttpMetricsHandler
  ): BidiFlow[HttpRequest, HttpRequest, HttpResponse, HttpResponse, NotUsed] = {
    val trace = BidiFlow.fromFlows(Flow[HttpRequest].map(traceRequest), Flow[HttpResponse])
    val meter = BidiFlow.fromGraph(new MeterStage(metricsHandler))
    trace.atop(meter)
  }

}

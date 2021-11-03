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

package fr.davit.akka.http.metrics.core.scaladsl

import akka.actor.ClassicActorSystemProvider
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, SystemMaterializer}
import fr.davit.akka.http.metrics.core.{HttpMetrics, HttpMetricsHandler}

import scala.annotation.nowarn
import scala.concurrent.Future

/** Metered server builder
  *
  * Use HttpExt.newMeteredServerAt() to create a builder, use methods to customize settings, and then call one of the
  * bind* methods to bind a server.
  *
  * Does not extend akka.http.scaladsl.ServerBuilder to seal routes internally in order to ensure proper metrics
  * instrumentations
  */
final case class HttpMetricsServerBuilder(
    interface: String,
    port: Int,
    metricsHandler: HttpMetricsHandler,
    context: ConnectionContext,
    log: LoggingAdapter,
    settings: ServerSettings,
    system: ClassicActorSystemProvider,
    materializer: Materializer
) {

  private lazy val http: HttpExt = Http(system)

  def onInterface(newInterface: String): HttpMetricsServerBuilder           = copy(interface = newInterface)
  def onPort(newPort: Int): HttpMetricsServerBuilder                        = copy(port = newPort)
  def meterTo(metricsHandler: HttpMetricsHandler): HttpMetricsServerBuilder = copy(metricsHandler = metricsHandler)
  def logTo(newLog: LoggingAdapter): HttpMetricsServerBuilder               = copy(log = newLog)
  def withSettings(newSettings: ServerSettings): HttpMetricsServerBuilder   = copy(settings = newSettings)
  def adaptSettings(f: ServerSettings => ServerSettings): HttpMetricsServerBuilder = copy(settings = f(settings))
  def enableHttps(newContext: HttpsConnectionContext): HttpMetricsServerBuilder    = copy(context = newContext)
  def withMaterializer(newMaterializer: Materializer): HttpMetricsServerBuilder = copy(materializer = newMaterializer)

  @nowarn("msg=deprecated")
  def connectionSource(): Source[Http.IncomingConnection, Future[ServerBinding]] =
    http
      .bind(interface, port, context, settings, log)
      .map(c => c.copy(_flow = c._flow.join(HttpMetrics.meterFlow(metricsHandler))))

  @nowarn("msg=deprecated")
  def bindFlow(route: Route): Future[ServerBinding] = {
    val flow        = HttpMetrics.metricsRouteToFlow(route)(system)
    val meteredFlow = HttpMetrics.meterFlow(metricsHandler).join(flow)
    http.bindAndHandle(
      meteredFlow,
      interface,
      port,
      context,
      settings,
      log
    )(materializer)
  }

  @nowarn("msg=deprecated")
  def bind(route: Route): Future[ServerBinding] = {
    val handler        = HttpMetrics.metricsRouteToFunction(route)(system)
    val meteredHandler = HttpMetrics.meterFunction(handler, metricsHandler)(materializer.executionContext)
    http.bindAndHandleAsync(
      meteredHandler,
      interface,
      port,
      context,
      settings,
      parallelism = 0,
      log
    )(materializer)
  }

  @nowarn("msg=deprecated")
  def bindSync(handler: HttpRequest => HttpResponse): Future[ServerBinding] = {
    val meteredHandler = HttpMetrics.meterFunctionSync(handler, metricsHandler)
    http.bindAndHandleSync(
      meteredHandler,
      interface,
      port,
      context,
      settings,
      log
    )(materializer)
  }
}

object HttpMetricsServerBuilder {

  def apply(
      interface: String,
      port: Int,
      metricsHandler: HttpMetricsHandler,
      system: ClassicActorSystemProvider
  ): HttpMetricsServerBuilder =
    HttpMetricsServerBuilder(
      interface,
      port,
      metricsHandler,
      HttpConnectionContext,
      system.classicSystem.log,
      ServerSettings(system.classicSystem),
      system,
      SystemMaterializer(system).materializer
    )
}

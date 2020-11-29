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

package akka.http.scaladsl

import akka.actor.ClassicActorSystemProvider
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{Materializer, SystemMaterializer}
import fr.davit.akka.http.metrics.core.{HttpMetrics, HttpMetricsHandler}

import scala.concurrent.{Future, Promise}

/**
  * Metered server builder
  * Defined in the akka.http.scaladsl package to get access to the HttpExt
  * to avoid deprecation warnings.
  *
  * Use HttpExt.newMeteredServerAt() to create a builder, use methods to customize settings,
  * and then call one of the bind* methods to bind a server.
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
) extends ServerBuilder {

  private lazy val http: HttpExt = Http(system)

  def onInterface(newInterface: String): ServerBuilder                      = copy(interface = newInterface)
  def onPort(newPort: Int): ServerBuilder                                   = copy(port = newPort)
  def meterTo(metricsHandler: HttpMetricsHandler): HttpMetricsServerBuilder = copy(metricsHandler = metricsHandler)
  def logTo(newLog: LoggingAdapter): ServerBuilder                          = copy(log = newLog)
  def withSettings(newSettings: ServerSettings): ServerBuilder              = copy(settings = newSettings)
  def adaptSettings(f: ServerSettings => ServerSettings): ServerBuilder     = copy(settings = f(settings))
  def enableHttps(newContext: HttpsConnectionContext): ServerBuilder        = copy(context = newContext)
  def withMaterializer(newMaterializer: Materializer): ServerBuilder        = copy(materializer = newMaterializer)

  def connectionSource(): Source[Http.IncomingConnection, Future[ServerBinding]] =
    http
      .bindImpl(interface, port, context, settings, log)
      .map(c => c.copy(_flow = c._flow.join(HttpMetrics.meterFlow(metricsHandler))))

  def bindFlow(handlerFlow: Flow[HttpRequest, HttpResponse, _]): Future[ServerBinding] =
    http.bindAndHandleImpl(
      HttpMetrics.meterFlow(metricsHandler).join(handlerFlow),
      interface,
      port,
      context,
      settings,
      log
    )(materializer)

  def bind(handler: HttpRequest => Future[HttpResponse]): Future[ServerBinding] =
    http.bindAndHandleAsyncImpl(
      HttpMetrics.meterFunction(handler, metricsHandler)(materializer),
      interface,
      port,
      context,
      settings,
      parallelism = 0,
      log
    )(materializer)

  def bindSync(handler: HttpRequest => HttpResponse): Future[ServerBinding] = bind { req =>
    val p = Promise[HttpResponse]()
    metricsHandler.onRequest(req, p.future)(materializer) // needs to be async
    p.success(handler(req))
    p.future
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

package fr.davit.akka.http.metrics.core.scaladsl.server

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, RoutingLog}
import akka.http.scaladsl.settings.{ParserSettings, RoutingSettings}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry

import scala.concurrent.duration.Deadline
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object HttpMetricsRoute {

  trait Settings {
    def definedError: HttpResponse => Boolean
  }

  object DefaultSettings extends Settings {
    override val definedError: HttpResponse => Boolean = _.status match {
      case _: StatusCodes.ServerError => true
      case _                          => false
    }
  }

  implicit def apply(route: Route): HttpMetricsRoute = new HttpMetricsRoute(route)

}

/**
  * Typeclass to add the metrics capabilities to a route
  *
  */
class HttpMetricsRoute private (route: Route) extends HttpMetricsDirectives {

  private def metricsHandler(registry: HttpMetricsRegistry, settings: HttpMetricsRoute.Settings, handler: HttpRequest => Future[HttpResponse])(
      request: HttpRequest)(
      implicit
      executionContext: ExecutionContext
  ): Future[HttpResponse] = {
    registry.requests.inc()
    registry.active.inc()
    registry.receivedBytes.update(request.entity.contentLengthOption.getOrElse(0L))
    val start = Deadline.now
    handler(request).transform { r =>
      registry.duration.observe(Deadline.now - start)
      registry.active.dec()
      if (r.map(settings.definedError).getOrElse(true)) registry.errors.inc()
      for {
        r <- r.toOption
        l <- r.entity.contentLengthOption
      } yield registry.sentBytes.update(l)
      r
    }
  }

  def recordMetrics(
      registry: HttpMetricsRegistry,
      settings: HttpMetricsRoute.Settings = HttpMetricsRoute.DefaultSettings)(
      implicit
      routingSettings: RoutingSettings,
      parserSettings: ParserSettings,
      materializer: Materializer,
      routingLog: RoutingLog,
      executionContext: ExecutionContextExecutor = null,
      rejectionHandler: RejectionHandler = RejectionHandler.default,
      exceptionHandler: ExceptionHandler = null): Flow[HttpRequest, HttpResponse, NotUsed] = {

    // override the execution context passed as parameter
    val effectiveEC = if (executionContext ne null) executionContext else materializer.executionContext

    {
      implicit val executionContext: ExecutionContextExecutor = effectiveEC
      Flow[HttpRequest].mapAsync(1)(metricsHandler(registry, settings, Route.asyncHandler(route)))
    }
  }
}

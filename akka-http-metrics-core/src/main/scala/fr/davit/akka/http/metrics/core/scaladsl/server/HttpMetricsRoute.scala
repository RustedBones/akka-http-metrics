package fr.davit.akka.http.metrics.core.scaladsl.server

import akka.NotUsed
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, RoutingLog}
import akka.http.scaladsl.settings.{ParserSettings, RoutingSettings}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{PathDimension, StatusGroupDimension}

import scala.concurrent.duration.Deadline
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

import akka.http.scaladsl.model.Uri

object HttpMetricsRoute {

  implicit def apply(route: Route): HttpMetricsRoute = new HttpMetricsRoute(route)

}

/**
  * Typeclass to add the metrics capabilities to a route
  *
  */
class HttpMetricsRoute private (route: Route) extends HttpMetricsDirectives {

  private def buildPathLabel(path: Uri.Path, segmentLabels: List[SegmentLabelHeader]): PathDimension = {
    import fr.davit.akka.http.metrics.core.scaladsl.model.Extensions._
    val builder = new StringBuilder()
    val (rest, _) = segmentLabels.foldLeft((path, 0)) {
      case ((r, idx), l) =>
        builder.append(r.take(l.from - idx))
        builder.append(l.label)
        (r.drop(l.to - idx), l.to)
    }
    builder.append(rest)
    PathDimension(builder.result())
  }

  private def metricsHandler(
      registry: HttpMetricsRegistry,
      settings: HttpMetricsSettings,
      handler: HttpRequest => Future[HttpResponse])(request: HttpRequest)(
      implicit
      executionContext: ExecutionContext
  ): Future[HttpResponse] = {
    registry.active.inc()
    registry.requests.inc()
    registry.receivedBytes.update(request.entity.contentLengthOption.getOrElse(0L))
    val start    = Deadline.now
    val response = handler(request)
    // no need to handle failures at this point. They will fail the stream hence the server
    response.map { r =>
      // extract custom segment headers
      val (segmentLabels, headers) = r.headers.foldLeft((List.empty[SegmentLabelHeader], List.empty[HttpHeader])) {
        case ((sls, hs), h: SegmentLabelHeader) => (h :: sls, hs)
        case ((sls, hs), h: HttpHeader)         => (sls, h :: hs)
      }

      // compute dimensions
      val statusGroupDim = if (settings.includeStatusDimension) Some(StatusGroupDimension(r.status)) else None
      val pathDim        = if (settings.includePathDimension) Some(buildPathLabel(request.uri.path, segmentLabels)) else None
      val dimensions     = statusGroupDim.toSeq ++ pathDim

      registry.active.dec()
      registry.responses.inc(dimensions)
      registry.duration.observe(Deadline.now - start, dimensions)
      if (settings.defineError(r)) registry.errors.inc(dimensions)
      r.entity.contentLengthOption.foreach(registry.sentBytes.update(_, dimensions))
      r.withHeaders(headers) // clean the custom headers
    }
  }

  def recordMetrics(registry: HttpMetricsRegistry, settings: HttpMetricsSettings = HttpMetricsSettings.default)(
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
      Flow[HttpRequest]
        .mapAsync(1)(recordMetricsAsync(registry, settings))
        .watchTermination() {
          case (mat, completion) =>
            // every connection materializes a stream
            registry.connections.inc()
            registry.connected.inc()
            completion.onComplete(_ => registry.connected.dec())
            mat
        }
    }
  }

  def recordMetricsAsync(registry: HttpMetricsRegistry, settings: HttpMetricsSettings = HttpMetricsSettings.default)(
      implicit
      routingSettings: RoutingSettings,
      parserSettings: ParserSettings,
      materializer: Materializer,
      routingLog: RoutingLog,
      executionContext: ExecutionContextExecutor = null,
      rejectionHandler: RejectionHandler = RejectionHandler.default,
      exceptionHandler: ExceptionHandler = null): HttpRequest => Future[HttpResponse] = {
    metricsHandler(registry, settings, Route.asyncHandler(route))
  }
}

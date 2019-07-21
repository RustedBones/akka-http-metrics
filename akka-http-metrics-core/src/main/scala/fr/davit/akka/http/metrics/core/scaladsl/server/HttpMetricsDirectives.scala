package fr.davit.akka.http.metrics.core.scaladsl.server

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
import akka.http.scaladsl.server.directives.BasicDirectives.{mapRequestContext, tprovide}
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.server.util.Tuple
import akka.http.scaladsl.server.{Directive, PathMatcher, StandardRoute}
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry

import scala.util.Try

final case class SegmentLabelHeader(from: Int, to: Int, label: String) extends ModeledCustomHeader[SegmentLabelHeader] {
  override def renderInRequests  = true
  override def renderInResponses = true
  override val companion         = SegmentLabelHeader
  override def value: String     = s"$from:$to:$label"
}

object SegmentLabelHeader extends ModeledCustomHeaderCompanion[SegmentLabelHeader] {
  override val name                 = "x-segment-label"
  override def parse(value: String) = Try(SegmentLabelHeader(value))
}

trait HttpMetricsDirectives {

  def metrics[T <: HttpMetricsRegistry: ToEntityMarshaller](registry: T): StandardRoute = complete(registry)

  def pathLabeled[L](pm: PathMatcher[L], label: String): Directive[L] = {
    pathPrefixLabeled(pm ~ PathEnd, label)
  }

  def pathPrefixLabeled[L](pm: PathMatcher[L], label: String): Directive[L] = {
    rawPathPrefixLabeled(Slash ~ pm, label)
  }

  def rawPathPrefixLabeled[L](pm: PathMatcher[L], label: String): Directive[L] = {
    implicit val LIsTuple: Tuple[L] = pm.ev
    extractRequestContext.flatMap { ctx =>
      extractMatchedPath
      pm(ctx.unmatchedPath) match {
        case Matched(rest, values) =>
          tprovide(values) & mapRequestContext(_ withUnmatchedPath rest) & mapResponse { response =>
            val path = ctx.request.uri.path
            val from = path.length - ctx.unmatchedPath.length
            val to = path.length - rest.length
            response.addHeader(new SegmentLabelHeader(from, to, "/" + label)) // path matchers always match the / prefix
          }
        case Unmatched =>
          reject
      }
    }
  }
}

object HttpMetricsDirectives extends HttpMetricsDirectives

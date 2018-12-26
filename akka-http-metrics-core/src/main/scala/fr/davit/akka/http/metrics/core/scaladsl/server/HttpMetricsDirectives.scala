package fr.davit.akka.http.metrics.core.scaladsl.server

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.StandardRoute
import akka.http.scaladsl.server.Directives._
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry

trait HttpMetricsDirectives {

  def metrics[T <: HttpMetricsRegistry: ToEntityMarshaller](registry: T): StandardRoute = complete(registry)

}

object HttpMetricsDirectives extends HttpMetricsDirectives

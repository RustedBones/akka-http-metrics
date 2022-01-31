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

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
import akka.http.scaladsl.server.directives.BasicDirectives.{mapRequestContext, tprovide}
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.server.util.Tuple
import akka.http.scaladsl.server.{Directive, PathMatcher, StandardRoute}
import fr.davit.akka.http.metrics.core.HttpMetrics.RequestPath
import fr.davit.akka.http.metrics.core.{HttpMetrics, HttpMetricsRegistry}

trait HttpMetricsDirectives {

  def metrics[T <: HttpMetricsRegistry: ToEntityMarshaller](registry: T): StandardRoute = complete(registry)

  def pathLabeled[L](pm: PathMatcher[L]): Directive[L] =
    pathPrefixLabeled(pm ~ PathEnd)

  def pathLabeled[L](pm: PathMatcher[L], label: String): Directive[L] =
    pathPrefixLabeled(pm ~ PathEnd, label)

  def pathPrefixLabeled[L](pm: PathMatcher[L]): Directive[L] =
    rawPathPrefixLabeled(Slash ~ pm)

  def pathPrefixLabeled[L](pm: PathMatcher[L], label: String): Directive[L] =
    rawPathPrefixLabeled(Slash ~ pm, label)

  def rawPathPrefixLabeled[L](pm: PathMatcher[L]): Directive[L] =
    rawPathPrefixLabeled(pm, None)

  def rawPathPrefixLabeled[L](pm: PathMatcher[L], label: String): Directive[L] =
    rawPathPrefixLabeled(pm, Some(label))

  private def rawPathPrefixLabeled[L](pm: PathMatcher[L], label: Option[String]): Directive[L] = {
    implicit val LIsTuple: Tuple[L] = pm.ev
    extractRequestContext.flatMap { ctx =>
      val pathCandidate = ctx.unmatchedPath.toString
      pm(ctx.unmatchedPath) match {
        case Matched(rest, values) =>
          tprovide(values) & mapRequestContext(_ withUnmatchedPath rest) & mapResponse { response =>
            val suffix = response.attribute(HttpMetrics.RequestPath.Key).map(_.label).getOrElse("")
            val pathLabel = label match {
              case Some(l) => "/" + l + suffix // pm matches additional slash prefix
              case None    => pathCandidate.substring(0, pathCandidate.length - rest.charCount) + suffix
            }
            response.addAttribute(HttpMetrics.RequestPath.Key, RequestPath(pathLabel))
          }
        case Unmatched =>
          reject
      }
    }
  }
}

object HttpMetricsDirectives extends HttpMetricsDirectives

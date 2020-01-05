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
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry
import fr.davit.akka.http.metrics.core.scaladsl.model.SegmentLabelHeader

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

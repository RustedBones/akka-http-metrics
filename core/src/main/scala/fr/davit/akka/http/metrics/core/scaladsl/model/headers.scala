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

package fr.davit.akka.http.metrics.core.scaladsl.model

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}

import scala.util.Try

private[core] final case class PathLabelHeader(label: String) extends ModeledCustomHeader[PathLabelHeader] {
  override def renderInRequests  = true
  override def renderInResponses = true
  override val companion         = PathLabelHeader
  override def value: String     = label
}

private[core] object PathLabelHeader extends ModeledCustomHeaderCompanion[PathLabelHeader] {
  override val name                 = "x-path-label"
  override def parse(value: String) = Try(PathLabelHeader(value))
}

private[core] final case class SegmentLabelHeader(from: Int, to: Int, label: String)
    extends ModeledCustomHeader[SegmentLabelHeader] {
  override def renderInRequests  = true
  override def renderInResponses = true
  override val companion         = SegmentLabelHeader
  override def value: String     = s"$from:$to:$label"
}

private[core] object SegmentLabelHeader extends ModeledCustomHeaderCompanion[SegmentLabelHeader] {
  override val name                 = "x-segment-label"
  override def parse(value: String) = Try(SegmentLabelHeader(value))
}

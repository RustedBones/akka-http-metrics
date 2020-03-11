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

private[core] sealed abstract class PathLabelHeader extends ModeledCustomHeader[PathLabelHeader] {
  override def renderInRequests = false

  override def renderInResponses = false

  override val companion = PathLabelHeader
}

private[core] final case class FullPathLabelHeader(label: String) extends PathLabelHeader {
  override def value: String = label
}

private[core] final case class SubPathLabelHeader(path: String, label: String) extends PathLabelHeader {
  override def value: String = s"$path#$label"
}

private[core] object PathLabelHeader extends ModeledCustomHeaderCompanion[PathLabelHeader] {

  val Unhandled = FullPathLabelHeader("unhandled")

  override val name = "x-path-label"

  override def parse(value: String) = Try {
    val index = value.indexOf('#')
    if (index >= 0) {
      val (path, label) = value.splitAt(index)
      SubPathLabelHeader(path, label)
    } else {
      FullPathLabelHeader(value)
    }
  }
}

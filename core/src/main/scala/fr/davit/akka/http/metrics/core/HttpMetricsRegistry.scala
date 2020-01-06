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

package fr.davit.akka.http.metrics.core

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

object HttpMetricsRegistry {

  final case class PathDimension(value: String) extends Dimension {
    override def key = "path"
  }

  object StatusGroupDimension {
    sealed trait StatusGroup extends EnumEntry with Lowercase

    object StatusGroup extends Enum[StatusGroup] {

      def apply(status: StatusCode): StatusGroup = status match {
        case _: StatusCodes.Success     => `2xx`
        case _: StatusCodes.Redirection => `3xx`
        case _: StatusCodes.ClientError => `4xx`
        case _: StatusCodes.ServerError => `5xx`
        case _                          => Other
      }

      case object `2xx` extends StatusGroup
      case object `3xx` extends StatusGroup
      case object `4xx` extends StatusGroup
      case object `5xx` extends StatusGroup
      case object Other extends StatusGroup

      override val values: immutable.IndexedSeq[StatusGroup] = findValues
    }

    def apply(status: StatusCode): StatusGroupDimension = new StatusGroupDimension(StatusGroup(status))
  }

  final case class StatusGroupDimension(group: StatusGroupDimension.StatusGroup) extends Dimension {
    override def key: String   = "status"
    override def value: String = group.toString.toLowerCase
  }

}

trait HttpMetricsRegistry {

  //--------------------------------------------------------------------------------------------------------------------
  // requests
  //--------------------------------------------------------------------------------------------------------------------
  def active: Gauge

  def requests: Counter

  def receivedBytes: Histogram

  //--------------------------------------------------------------------------------------------------------------------
  // responses
  //--------------------------------------------------------------------------------------------------------------------
  def responses: Counter

  def errors: Counter

  def duration: Timer

  def sentBytes: Histogram

  //--------------------------------------------------------------------------------------------------------------------
  // Connections
  //--------------------------------------------------------------------------------------------------------------------
  def connected: Gauge

  def connections: Counter
}

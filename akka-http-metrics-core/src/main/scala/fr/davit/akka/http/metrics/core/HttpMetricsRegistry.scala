package fr.davit.akka.http.metrics.core

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import enumeratum.{Enum, EnumEntry}
import enumeratum.EnumEntry.Lowercase

import scala.collection.immutable

object HttpMetricsRegistry {

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
    override val key: String   = "status"
    override def value: String = group.toString.toLowerCase
  }

}

trait HttpMetricsRegistry {

  //--------------------------------------------------------------------------------------------------------------------
  // requests
  //--------------------------------------------------------------------------------------------------------------------
  def active: Gauge[Long]

  def requests: Counter[Long]

  def receivedBytes: Histogram[Long]

  //--------------------------------------------------------------------------------------------------------------------
  // responses
  //--------------------------------------------------------------------------------------------------------------------
  def responses: Counter[Long]

  def errors: Counter[Long]

  def duration: Timer

  def sentBytes: Histogram[Long]

  //--------------------------------------------------------------------------------------------------------------------
  // Connections
  //--------------------------------------------------------------------------------------------------------------------
  def connected: Gauge[Long]

  def connections: Counter[Long]
}

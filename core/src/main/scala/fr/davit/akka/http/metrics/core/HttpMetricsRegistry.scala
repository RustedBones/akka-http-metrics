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

import akka.Done
import akka.http.scaladsl.model._
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry.{PathDimension, StatusGroupDimension}
import fr.davit.akka.http.metrics.core.scaladsl.model.{PathLabelHeader, SegmentLabelHeader}
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsSettings

import scala.collection.immutable
import scala.concurrent.duration.Deadline
import scala.concurrent.{ExecutionContext, Future}

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

abstract class HttpMetricsRegistry(settings: HttpMetricsSettings) extends HttpMetricsHandler {

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

  private def buildPathLabel(
      path: Uri.Path,
      pathLabel: Option[PathLabelHeader],
      segmentLabels: Seq[SegmentLabelHeader]
  ): PathDimension = {
    import fr.davit.akka.http.metrics.core.scaladsl.model.Extensions._
    pathLabel match {
      case Some(label) =>
        PathDimension(label.value)
      case None =>
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
  }

  override def onRequest(request: HttpRequest, response: Future[HttpResponse])(
      implicit executionContext: ExecutionContext
  ): Unit = {
    active.inc()
    requests.inc()
    receivedBytes.update(request.entity.contentLengthOption.getOrElse(0L))
    val start = Deadline.now

    response.foreach { r =>
      // extract custom segment headers
      val pathLabel     = r.header[PathLabelHeader]
      val segmentLabels = r.headers[SegmentLabelHeader]

      // compute dimensions
      val statusGroupDim = if (settings.includeStatusDimension) Some(StatusGroupDimension(r.status)) else None
      val pathDim =
        if (settings.includePathDimension) Some(buildPathLabel(request.uri.path, pathLabel, segmentLabels)) else None
      val dimensions = statusGroupDim.toSeq ++ pathDim

      active.dec()
      responses.inc(dimensions)
      duration.observe(Deadline.now - start, dimensions)
      if (settings.defineError(r)) {
        errors.inc(dimensions)
      }
      r.entity.contentLengthOption.foreach(sentBytes.update(_, dimensions))
    }
  }

  override def onConnection(completion: Future[Done])(implicit executionContext: ExecutionContext): Unit = {
    connections.inc()
    connected.inc()
    completion.onComplete(_ => connected.dec())
  }
}

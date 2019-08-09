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

private[core] final case class SegmentLabelHeader(from: Int, to: Int, label: String) extends ModeledCustomHeader[SegmentLabelHeader] {
  override def renderInRequests  = true
  override def renderInResponses = true
  override val companion         = SegmentLabelHeader
  override def value: String     = s"$from:$to:$label"
}

private[core] object SegmentLabelHeader extends ModeledCustomHeaderCompanion[SegmentLabelHeader] {
  override val name                 = "x-segment-label"
  override def parse(value: String) = Try(SegmentLabelHeader(value))
}
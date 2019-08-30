package fr.davit.akka.http.metrics.prometheus.marshalling

import java.io.StringWriter

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpCharsets, HttpEntity, MediaTypes}
import fr.davit.akka.http.metrics.prometheus.PrometheusRegistry
import io.prometheus.client.exporter.common.TextFormat

trait PrometheusMarshallers {

  val PrometheusContentType: ContentType = {
    MediaTypes.`text/plain` withParams Map("version" -> "0.0.4") withCharset HttpCharsets.`UTF-8`
  }

  implicit val marshaller: ToEntityMarshaller[PrometheusRegistry] = {
    Marshaller.opaque { registry =>
      val output = new StringWriter()
      try {
        TextFormat.write004(output, registry.underlying.metricFamilySamples)
        HttpEntity(output.toString).withContentType(PrometheusContentType)
      } finally {
        output.close()
      }
    }
  }
}

object PrometheusMarshallers extends PrometheusMarshallers

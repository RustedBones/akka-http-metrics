package fr.davit.akka.http.metrics.dropwizard.marshalling

import java.io.StringWriter

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import com.fasterxml.jackson.databind.ObjectMapper
import fr.davit.akka.http.metrics.dropwizard.DropwizardRegistry

trait DropwizardMarshallers {

  implicit val registryToEntityMarshaller: ToEntityMarshaller[DropwizardRegistry] = {

    val writer = new ObjectMapper().writer()

    Marshaller.opaque { registry =>
      val output = new StringWriter()
      try {
        writer.writeValue(output, registry.underlying)
        HttpEntity(output.toString).withContentType(ContentTypes.`application/json`)
      } finally {
        output.close()
      }
    }
  }
}

object DropwizardMarshallers extends DropwizardMarshallers

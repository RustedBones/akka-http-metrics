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

package fr.davit.akka.http.metrics.prometheus.marshalling

import java.io.StringWriter

import org.apache.pekko.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.{ContentType, HttpCharsets, HttpEntity, MediaTypes}
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

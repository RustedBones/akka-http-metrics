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

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.impl.engine.http2.TelemetrySpi
import akka.http.scaladsl.model.{AttributeKey, HttpMethod, HttpRequest, HttpResponse, RequestResponseAssociation}
import akka.stream.scaladsl.{BidiFlow, Flow, Tcp}

import scala.concurrent.duration.Deadline

object HttpMetrics {

  final case class RequestMethod(method: HttpMethod) extends RequestResponseAssociation
  object RequestMethod {
    val Key: AttributeKey[RequestMethod] = AttributeKey[RequestMethod]("association-request-method")
  }

  final case class RequestTimestamp(value: Deadline) extends RequestResponseAssociation
  object RequestTimestamp {
    val Key: AttributeKey[RequestTimestamp] = AttributeKey[RequestTimestamp]("association-request-timestamp")
  }

  final case class RequestPath(label: String) extends RequestResponseAssociation
  object RequestPath {
    val Key: AttributeKey[RequestPath] = AttributeKey[RequestPath]("association-request-path")
  }

  abstract class Telemetry(system: ActorSystem) extends TelemetrySpi {

    import system._

    def clientMetrics: HttpMetricsHandler
    def serverMetrics: HttpMetricsHandler

    override def client: BidiFlow[HttpRequest, HttpRequest, HttpResponse, HttpResponse, NotUsed] = {
      BidiFlow.fromFunctions(clientMetrics.onRequest, clientMetrics.onResponse)
    }

    override def serverBinding: Flow[Tcp.IncomingConnection, Tcp.IncomingConnection, NotUsed] = {
      Flow[Tcp.IncomingConnection]
        .map { c =>
          val meteredFlow = c.flow.watchTermination() { (notUsed, done) =>
            serverMetrics.onConnection()
            done.onComplete(_ => serverMetrics.onDisconnection())
            notUsed
          }
          c.copy(flow = meteredFlow)
        }
    }

    override def serverConnection: BidiFlow[HttpResponse, HttpResponse, HttpRequest, HttpRequest, NotUsed] = {
      BidiFlow.fromFunctions(serverMetrics.onResponse, serverMetrics.onRequest)
    }

  }

}

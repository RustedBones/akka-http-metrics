package fr.davit.akka.http.metrics.graphite

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source, Tcp}
import akka.util.ByteString
import fr.davit.akka.http.metrics.core.Dimension

object CarbonClient {

  def apply(host: String, port: Int)(implicit system: ActorSystem): CarbonClient = new CarbonClient(host, port)
}

class CarbonClient(host: String, port: Int)(implicit system: ActorSystem) {

  implicit private lazy val materializer: Materializer = ActorMaterializer()

  private def serialize[T](name: String, value: T, dimensions: Seq[Dimension], ts: Instant): ByteString = {
    val tags = dimensions.map(d => d.key + "=" + d.value).toList
    val identifier = (name :: tags).mkString(";")
    ByteString(s"$identifier $value ${ts.getEpochSecond}")
  }

  private val queue = Source.queue[ByteString](19, OverflowStrategy.dropNew)
    .via(Tcp().outgoingConnection(host, port))
    .toMat(Sink.ignore)(Keep.left)
    .run()

  def publish[T](name: String, value: T, dimensions: Dimension*): Unit = {
    queue.offer(serialize(name, value, dimensions.toSeq, Instant.now()))
  }
}

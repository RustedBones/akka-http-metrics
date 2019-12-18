package fr.davit.akka.http.metrics.graphite

import java.time.{Clock, Instant}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Flow, Keep, RestartFlow, Sink, Source, Tcp}
import akka.stream.{OverflowStrategy, QueueOfferResult}
import akka.util.ByteString

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}

object CarbonClient {

  def apply(host: String, port: Int)(implicit system: ActorSystem): CarbonClient = new CarbonClient(host, port)
}

class CarbonClient(host: String, port: Int)(implicit system: ActorSystem) extends AutoCloseable {

  private val logger = Logging(system.eventStream, classOf[CarbonClient])
  protected val clock: Clock = Clock.systemUTC()

  private def serialize[T](name: String, value: T, ts: Instant): ByteString = {
    ByteString(s"$name $value ${ts.getEpochSecond}\n")
  }

  // TODO read backoff from config
  private def connection: Flow[ByteString, ByteString, NotUsed]  = RestartFlow.withBackoff(
    minBackoff = 3.seconds,
    maxBackoff = 30.seconds,
    randomFactor = 0.2, // adds 20% "noise" to vary the intervals slightly
    maxRestarts = -1 // keep retrying forever
  )(() => Tcp().outgoingConnection(host, port))

  private val queue = Source.queue[ByteString](19, OverflowStrategy.dropHead)
    .via(connection)
    .toMat(Sink.ignore)(Keep.left)
    .run()

  def publish[T](name: String, value: T, ts: Instant = Instant.now(clock)): Unit = {
    // it's reasonable to block until the message in enqueued
    Await.result(queue.offer(serialize(name, value, ts)), Duration.Inf) match {
      case QueueOfferResult.Enqueued => logger.debug("Metric {} enqueued", name)
      case QueueOfferResult.Dropped => logger.debug("Metric {} dropped", name)
      case QueueOfferResult.Failure(e) => logger.error(e, s"Failed publishing metric $name")
      case QueueOfferResult.QueueClosed => throw new Exception("Failed publishing metric to closed carbon client")
    }
  }

  override def close(): Unit = {
    queue.complete()
    Await.result(queue.watchCompletion(), Duration.Inf)
  }
}

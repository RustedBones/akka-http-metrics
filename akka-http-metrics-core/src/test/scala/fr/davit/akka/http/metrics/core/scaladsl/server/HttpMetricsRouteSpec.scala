package fr.davit.akka.http.metrics.core.scaladsl.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext, RouteResult}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import fr.davit.akka.http.metrics.core.TestRegistry
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsRoute._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

class HttpMetricsRouteSpec
    extends TestKit(ActorSystem("HttpMetricsRouteSpec"))
    with FlatSpecLike
    with Matchers
    with Eventually
    with ScalaFutures
    with MockFactory
    with BeforeAndAfterAll {

  import Directives._

  implicit val _            = system
  implicit val materializer = ActorMaterializer()

  abstract class Fixture[T](testField: TestRegistry => T) {
    implicit val registry = new TestRegistry

    val server = mockFunction[RequestContext, Future[RouteResult]]

    val (source, sink) = TestSource
      .probe[HttpRequest]
      .via(server.recordMetrics(registry))
      .toMat(TestSink.probe[HttpResponse])(Keep.both)
      .run()

    def actual: T = testField(registry)
  }

  override def afterAll(): Unit = {
    shutdown()
    super.afterAll()
  }

  "HttpMetricsRoute" should "compute the number of requests" in new Fixture(_.requests) {
    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.OK))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value shouldBe 1

    sink.request(1)
    server.expects(*).onCall(reject)
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value shouldBe 2

    sink.request(1)
    server.expects(*).onCall(failWith(new Exception("BOOM!")))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value shouldBe 3
  }

  it should "compute the number of errors" in new Fixture(_.errors) {
    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.OK))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value shouldBe 0

    sink.request(1)
    server.expects(*).onCall(reject)
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value shouldBe 0

    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.InternalServerError))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value shouldBe 1

    sink.request(1)
    server.expects(*).onCall(failWith(new Exception("BOOM!")))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.value shouldBe 2
  }

  it should "compute the number of active requests" in new Fixture(_.active) {
    val promise = Promise[StatusCode]()

    sink.request(1)
    server.expects(*).onCall(complete(promise.future))
    source.sendNext(HttpRequest())

    // wait for the request to be sent to the handler
    eventually(actual.value shouldBe 1)

    promise.success(StatusCodes.OK)
    sink.expectNext()
    actual.value shouldBe 0
  }

  it should "compute the requests time" in new Fixture(_.duration) {
    val promise  = Promise[StatusCode]()
    val duration = 500.millis

    sink.request(1)
    server.expects(*).onCall(complete(promise.future))
    source.sendNext(HttpRequest())
    system.scheduler.scheduleOnce(duration)(promise.success(StatusCodes.OK))(system.dispatcher)
    sink.expectNext(duration * 2)
    actual.values.head should be > duration
  }

  it should "compute the requests size" in new Fixture(_.receivedBytes) {
    val data = "This is the request content"

    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.OK))
    source.sendNext(HttpRequest(entity = HttpEntity(data)))
    sink.expectNext()
    actual.values.head shouldBe data.getBytes.length
  }

  it should "compute the response size" in new Fixture(_.sentBytes) {
    val data = "This is the response content"

    sink.request(1)
    server.expects(*).onCall(complete(StatusCodes.OK -> data))
    source.sendNext(HttpRequest())
    sink.expectNext()
    actual.values.head shouldBe data.getBytes.length
  }

  it should "compute the number of active connections" in {
    implicit val registry = new TestRegistry
    val server = mockFunction[RequestContext, Future[RouteResult]]

    val stream = Source.maybe.via(server.recordMetrics(registry)).toMat(Sink.ignore)(Keep.both)
    val conns  = (0 until 3).map(_ => stream.run())
    registry.connected.value shouldBe 3

    conns.map { case (c, _) => c.success(None) }
    Future.sequence(conns.map { case (_, completion) => completion }).futureValue
    registry.connected.value shouldBe 0
  }

  it should "compute the number of connections" in  {
    implicit val registry = new TestRegistry
    val server = mockFunction[RequestContext, Future[RouteResult]]

    val stream = Source((0 until 5).map(_ => HttpRequest())).via(server.recordMetrics(registry)).toMat(Sink.ignore)(Keep.right)
    val completions = (0 until 3).map(_ => stream.run())
    Future.sequence(completions).futureValue
    registry.connections.value shouldBe 3
  }
}

package fr.davit.akka.http.metrics.core

import akka.Done
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

trait HttpMetricsHandler {

  def onRequest(request: HttpRequest, response: Future[HttpResponse])(implicit executionContext: ExecutionContext): Unit

  def onConnection(completion: Future[Done])(implicit executionContext: ExecutionContext): Unit
}

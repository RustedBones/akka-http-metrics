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

package fr.davit.pekko.http.metrics.core

import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}

trait HttpMetricsHandler {

  def onRequest(request: HttpRequest): HttpRequest

  def onResponse(request: HttpRequest, response: HttpResponse): HttpResponse

  def onFailure(request: HttpRequest, cause: Throwable): Throwable

  def onConnection(): Unit

  def onDisconnection(): Unit
}

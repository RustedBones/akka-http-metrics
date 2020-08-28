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

trait HttpMetricsNames {
  def requests: String
  def activeRequests: String
  def requestSizes: String
  def responses: String
  def errors: String
  def durations: String
  def responseSizes: String
  def connections: String
  def activeConnections: String

  def withRequests(name: String): HttpMetricsNames
  def withActiveRequests(name: String): HttpMetricsNames
  def withRequestSizes(name: String): HttpMetricsNames
  def withResponses(name: String): HttpMetricsNames
  def withErrors(name: String): HttpMetricsNames
  def withDurations(name: String): HttpMetricsNames
  def withResponseSizes(name: String): HttpMetricsNames
  def withConnections(name: String): HttpMetricsNames
  def withActiveConnections(name: String): HttpMetricsNames

}

object HttpMetricsNames {

  private[metrics] case class HttpMetricsNamesImpl(
      requests: String,
      activeRequests: String,
      requestSizes: String,
      responses: String,
      errors: String,
      durations: String,
      responseSizes: String,
      connections: String,
      activeConnections: String
  ) extends HttpMetricsNames {
    def withRequests(name: String): HttpMetricsNamesImpl          = copy(requests = name)
    def withActiveRequests(name: String): HttpMetricsNamesImpl    = copy(activeRequests = name)
    def withRequestSizes(name: String): HttpMetricsNamesImpl      = copy(requestSizes = name)
    def withResponses(name: String): HttpMetricsNamesImpl         = copy(responses = name)
    def withErrors(name: String): HttpMetricsNamesImpl            = copy(errors = name)
    def withDurations(name: String): HttpMetricsNamesImpl         = copy(durations = name)
    def withResponseSizes(name: String): HttpMetricsNamesImpl     = copy(responseSizes = name)
    def withConnections(name: String): HttpMetricsNamesImpl       = copy(connections = name)
    def withActiveConnections(name: String): HttpMetricsNamesImpl = copy(activeConnections = name)
  }

}

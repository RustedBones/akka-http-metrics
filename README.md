# akka-http-metrics

[![Build Status](https://travis-ci.org/RustedBones/akka-http-metrics.svg?branch=master&style=flat)](https://travis-ci.org/RustedBones/akka-http-metrics)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/fr.davit/akka-http-metrics-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/fr.davit/akka-http-metrics-core_2.12)
[![Software License](https://img.shields.io/badge/license-Apache%202-brightgreen.svg?style=flat)](LICENSE)

Easily collect and expose metrics in your akka-http server.

The following implementations are supported:

* [datadog](#datadog) (via StatsD)
* [dropwizard](#dropwizard)
* [prometheus](#prometheus)

## Versions

| Version | Release date | Akka Http version | Scala versions      |
| ------- | ------------ | ----------------- | ------------------- |
| `0.2.0` | 2018-12-28   | `10.1.6`          | `2.11.12`, `2.12.8` |
| `0.2.1` | 2019-03-23   | `10.1.8`          | `2.11.12`, `2.12.8` |

The complete list can be found in the [CHANGELOG](CHANGELOG.md) file.

## Getting akka-http-metrics

Libraries are published to Maven Central. Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-<backend>" % <version>
```

**Important**: Since akka-http 10.1.0, akka-stream transitive dependency is marked as provided. You should now explicitly
include it in your build.

> [...] we changed the policy not to depend on akka-stream explicitly anymore but mark it as a provided dependency in our build. 
That means that you will always have to add a manual dependency to akka-stream. Please make sure you have chosen and 
added a dependency to akka-stream when updating to the new version

```scala
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % <version> // Only Akka 2.5 supported
```

For more details, see the akka-http 10.1.x [release notes](https://doc.akka.io/docs/akka-http/current/release-notes/10.1.x.html)


### Server metrics

The library enables you to easily record the following metrics from an akka-http server into a registry. The
following metrics are recorded:

- requests (`counter`)
- active requests (`gauge`)
- request sizes (`histogram`)
- responses (`counter`)
    - status group [1xx/|2xx|3xx|4xx|5xx|other]
- errors
- durations (`histogram`)
    - status group [1xx/|2xx|3xx|4xx|5xx|other]
- response sizes (`histogram`)

Record metrics from your akka server by importing the implicits from `HttpMetricsRoute`. Convert your route to the
flow that will handle requests with `recordMetrics` and bind your server to the desired port.

```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import fr.davit.akka.http.metrics.core.HttpMetricsRegistry
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsRoute._

implicit val system = ActorSystem()
implicit val materializer = ActorMaterializer()

val registry: HttpMetricsRegistry = ... // concrete registry implementation

val route: Route = ... // your route

Http().bindAndHandle(route.recordMetrics(registry), "localhost", 8080)
```

By default, the errored request counter will be incremented when the served response is an `Server error (5xx)`.
You can override this behaviour in the settings.

```scala
val settings = HttpMetricsSettings.default.withDefineError(_.status.isFailure)

Http().bindAndHandle(route.recordMetrics(registry, settings), "localhost", 8080)
```

In this example, all responses with status >= 400 are considered as errors.

### Expose metrics

Expose the metrics from the registry on an http endpoint with the `metrics` directive.

```scala
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._

val route = (get & path("metrics")) {
  metrics(registry)
}
```

Of course, you will also need to have the implicit marshaller for your registry in scope.


## Implementations

### [Datadog]( https://docs.datadoghq.com/developers/dogstatsd/)

| metric          | name                             |
|-----------------|----------------------------------|
| requests        | akka.http.requests_count         |
| active requests | akka.http.requests_active        |
| request sizes   | akka.http.requests_bytes         |
| responses       | akka.http.responses_count        |
| errors          | akka.http.responses_errors_count |
| durations       | akka.http.responses_duration     |
| response sizes  | akka.http.response_bytes         |

The `DatadogRegistry` is just a facade to publish to your StatsD server. The registry itself not located in the JVM, 
for this reason it is not possible to expose the metrics in your API.

Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-datadog" % <version>
```

Create your registry

```scala
import com.timgroup.statsd.StatsDClient
import fr.davit.akka.http.metrics.datadog.DatadogRegistry

val client: StatsDClient = ... // your statsd client

val registry = DatadogRegistry(client)
```

See datadog's [documentation](https://github.com/dataDog/java-dogstatsd-client) on how to create a StatsD client.


### [Dropwizard](https://metrics.dropwizard.io/)

| metric          | dropwizard                   |
|-----------------|------------------------------|
| requests        | akka.http.requests           |
| active requests | akka.http.requests.active    |
| request sizes   | akka.http.requests.bytes     |
| responses       | akka.http.responses          |
| errors          | akka.http.responses.errors   |
| durations       | akka.http.responses.duration |
| response sizes  | akka.http.responses.bytes    |

**Important**: The `DropwizardRegistry` works with tags. This feature is only supported since dropwizard `v5`. 

Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-dropwizard" % <version>
```

Create your registry

```scala
import io.dropwizard.metrics5.MetricRegistry
import fr.davit.akka.http.metrics.dropwizard.DropwizardRegistry

val dropwizard: MetricRegistry = ... // your dropwizard registry

val registry = DropwizardRegistry(dropwizard) // or DropwizardRegistry() to use a fresh registry
```

Expose the metrics

```scala
import fr.davit.akka.http.metrics.dropwizard.marshalling.DropwizardMarshallers._

val route = (get & path("metrics"))(metrics(registry))
```

### [Prometheus](http://prometheus.io/)

| metric          | prometheus                           |
|-----------------|--------------------------------------|
| requests        | akka_http_requests_total             |
| active requests | akka_http_requests_active            |
| request sizes   | akka_http_requests_size_bytes        |
| responses       | akka_http_responses_total            |
| errors          | akka_http_responses_errors_total     |
| durations       | akka_http_responses_duration_seconds |
| response sizes  | akka_http_responses_size_bytes       |

Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-prometheus" % <version>
```

Create your registry

```scala
import io.prometheus.client.CollectorRegistry
import fr.davit.akka.http.metrics.prometheus.PrometheusRegistry

val prometheus: CollectorRegistry = ... // your prometheus registry

val registry = PrometheusRegistry(prometheus) // or PrometheusRegistry() to use the default registry
```

Expose the metrics

```scala
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers._

val route = (get & path("metrics"))(metrics(registry))
```

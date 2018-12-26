# akka-http-metrics

[![Build Status](https://travis-ci.org/RustedBones/akka-http-metrics.svg?branch=master&style=flat)](https://travis-ci.org/RustedBones/akka-http-metrics)
[![Software License](https://img.shields.io/badge/license-Apache%202-brightgreen.svg?style=flat)](LICENSE)

Easily collect and expose metrics in your akka-http server.

The following implementations are supported:

* [datadog](#datadog) (via StatsD)
* [dropwizard](#dropwizard)
* [prometheus](#prometheus)

## Versions

| Version | Release date | Akka Http version | Scala versions      |
| ------- | ------------ | ----------------- | ------------------- |
| `0.2.0` |              | `10.1.6`          | `2.11.13`, `2.12.8` |

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
metric names are chosen according to the backend naming guidelines.

|metric                         |type     |datadog                        |dropwizard                  |prometheus                         |
|-------------------------------|---------|-------------------------------|----------------------------|-----------------------------------|
| served requests (counter)     |counter  |akka.http.requests_count       |akka.http.requests          |akka_http_requests_total           |
| errored request (counter)     |counter  |akka.http.requests_errors_count|akka.http.requests.errors   |akka_http_requests_errors_total    |
| active requests (gauge)       |gauge    |akka.http.requests_active      |akka.http.requests.active   |akka_http_requests_active          |
| requests durations (histogram)|histogram|akka.http.requests_time        |akka.http.requests.durations|akka_http_requests_duration_seconds|
| request sizes (histogram)     |histogram|akka.http.requests_bytes       |akka.http.requests.sizes    |akka_http_requests_size_bytes      |
| response sizes (histogram)    |histogram|akka.http.responses_bytes      |akka.http.responses.sizes   |akka_http_requests_size_bytes      |


## Quick start

### Record metrics

To enable the feature, import the implicits from `HttpMetricsRoute` and start your server adding the `recordMetrics` to your route.

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

### Expose metrics

If you want to expose the metrics from the registry on you endpoint, add the `metrics` directive to your route

```scala
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._

val route = (get & path("metrics")) {
  metrics(registry)
}
```

Of course, you will also need an implicit marshaller for your registry implementation.


## Implementations

### [Datadog]( https://docs.datadoghq.com/developers/dogstatsd/)

The `DatadogRegistry` is just a facade to publish to your StatsD server. The registry is not stored in the JVM, for this reason, 
it is not possible to expose the metrics in your API.

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

### [Dropwizard](https://metrics.dropwizard.io/)

Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-dropwizard" % <version>
```

Create your registry

```scala
import com.codahale.metrics.MetricRegistry
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

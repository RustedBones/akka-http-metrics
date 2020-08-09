# akka-http-metrics
[![Scala CI](https://github.com/RustedBones/akka-http-metrics/workflows/Scala%20CI/badge.svg)](https://github.com/RustedBones/akka-http-metrics/actions?query=workflow%3A"Scala+CI")
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/fr.davit/akka-http-metrics-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/fr.davit/akka-http-metrics-core_2.12)
[![Software License](https://img.shields.io/badge/license-Apache%202-brightgreen.svg?style=flat)](LICENSE)

Easily collect and expose metrics in your akka-http server.

The following implementations are supported:

* [datadog](#datadog) (via StatsD)
* [dropwizard](#dropwizard)
* [graphite](#graphite) (via Carbon)
* [prometheus](#prometheus)

## Versions

| Version | Release date | Akka Http version | Scala versions      |
| ------- | ------------ | ----------------- | ------------------- |
| `1.1.1` | 2020-06-10   | `10.1.12`         | `2.13.2`, `2.12.11` |
| `1.1.0` | 2020-04-18   | `10.1.11`         | `2.13.1`, `2.12.10` |
| `1.0.0` | 2020-03-14   | `10.1.11`         | `2.13.1`, `2.12.10` |


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
following labeled metrics are recorded:

- requests (`counter`)
- active requests (`gauge`)
- request sizes (`histogram`)
- responses (`counter`) [status group | path]
- errors [status group | path]
- durations (`histogram`) [status group | path]
- response sizes (`histogram`) [status group | path]
- connections (`counter`)
- active connections (`gauge`)

Record metrics from your akka server by importing the implicits from `HttpMetricsRoute`. Convert your route to the
flow that will handle requests with `recordMetrics` and bind your server to the desired port.

```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import fr.davit.akka.http.metrics.core.{HttpMetricsRegistry, HttpMetricsSettings}
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsRoute._

implicit val system = ActorSystem()
implicit val materializer = ActorMaterializer()

val settings: HttpMetricsSettings = HttpMetricsSettings
                                      .default
                                      .withNamespace("com.example.service")

val registry: HttpMetricsRegistry = ... // concrete registry implementation

val route: Route = ... // your route
```

You can bind this route to a HTTP server as follows

```scala
//If you are using akka-http >= 10.2.0
Http().newServerAt("localhost", 8080).bindFlow(route.recordMetrics(registry))
```

```scala
//If you are using akka-http < 10.2.0
Http().bindAndHandle(route.recordMetrics(registry), "localhost", 8080)
```

By default, the errored request counter will be incremented when the served response is an `Server error (5xx)`.
You can override this behaviour in the settings.

```scala
val settings = HttpMetricsSettings
  .default
  .withDefineError(_.status.isFailure)
```

In this example, all responses with status >= 400 are considered as errors.

For HTTP2 you must convert the `Route` to the handler function with `recordMetricsAsync`. In this case the connection
metrics won't be available.

```scala
//If you are using akka-http >= 10.2.0
Http().newServerAt("localhost", 8080).bind(route.recordMetrics(registry))
```

```scala
//If you are using akka-http < 10.2.0
Http2().bindAndHandleAsync(route.recordMetricsAsync(registry), "localhost", 8080)
```

#### Labels

By default metrics labels are disabled. You can enable them in the settings.

```scala
val settings = HttpMetricsSettings.default
  .withIncludeMethodDimension(true)
  .withIncludePathDimension(true)
  .withIncludeStatusDimension(true)
```

##### Method

The method of the request is used as dimension on the metrics. eg. `GET`

##### Path

Matched path of the request is used as dimension on the metrics.

When enabled, all metrics will get `unlabelled` as path dimension by default,
You must use the labelled path directives defined in `HttpMetricsDirectives` to set the dimension value.

You must also be careful about cardinality: see [here](https://prometheus.io/docs/practices/naming/#labels).
If your path contains unbounded dynamic segments, you must give an explicit label to override the dynamic part:

```scala
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._

val route = pathPrefixLabel("api") {
  pathLabeled("user" / JavaUUID, "user/:user-id") { userId =>
    ...
  }
}
```

Moreover, all unhandled requests will have path dimension set to `unhandled`.

##### Status group

The status group creates the following dimensions on the metrics: `1xx|2xx|3xx|4xx|5xx|other`

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

| metric             | name                   |
|--------------------|------------------------|
| requests           | requests_count         |
| active requests    | requests_active        |
| request sizes      | requests_bytes         |
| responses          | responses_count        |
| errors             | responses_errors_count |
| durations          | responses_duration     |
| response sizes     | response_bytes         |
| connections        | connections_count      |
| active connections | connections_active     |

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

| metric             | name               |
|--------------------|--------------------|
| requests           | requests           |
| active requests    | requests.active    |
| request sizes      | requests.bytes     |
| responses          | responses          |
| errors             | responses.errors   |
| durations          | responses.duration |
| response sizes     | responses.bytes    |
| connections        | connections        |
| active connections | connections.active |

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

### [Graphite](https://graphiteapp.org/)

| metric             | name               |
|--------------------|--------------------|
| requests           | requests           |
| active requests    | requests.active    |
| request sizes      | requests.bytes     |
| responses          | responses          |
| errors             | responses.errors   |
| durations          | responses.duration |
| response sizes     | responses.bytes    |
| connections        | connections        |
| active connections | connections.active |

Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-graphite" % <version>
```

Create your carbon client and your registry

```scala
import fr.davit.akka.http.metrics.graphite.{CarbonClient, GraphiteRegistry}

val carbonClient: CarbonClient = CarbonClient("hostname", 2003)

val registry = GraphiteRegistry(carbonClient)
```

### [Prometheus](http://prometheus.io/)

| metric             | name                       |
|--------------------|----------------------------|
| requests           | requests_total             |
| active requests    | requests_active            |
| request sizes      | requests_size_bytes        |
| responses          | responses_total            |
| errors             | responses_errors_total     |
| durations          | responses_duration_seconds |
| response sizes     | responses_size_bytes       |
| connections        | connections_total          |
| active connections | connections_active         |

Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-prometheus" % <version>
```

Create your registry

```scala
import io.prometheus.client.CollectorRegistry
import fr.davit.akka.http.metrics.prometheus.{PrometheusRegistry, PrometheusSettings}

val settings: PrometheusSettings = ... // your http metrics settings
val prometheus: CollectorRegistry = ... // your prometheus registry

val registry = PrometheusRegistry(prometheus, settings) // or PrometheusRegistry(settings = settings) to use the default registry
```

You can fine-tune the `histogram/summary` configuration of `buckets/quantiles` for the `request
 sizes`, `durations` and `response sizes` metrics.
 
```scala
val settings: PrometheusSettings = PrometheusSettings
  .default
  .withDurationConfig(Buckets(1, 2, 3, 5, 8, 13, 21, 34))
  .withReceivedBytesConfig(Quantiles(0.5, 0.75, 0.9, 0.95, 0.99))
  .withSentBytesConfig(PrometheusSettings.DefaultQuantiles)
```


Expose the metrics

```scala
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers._

val route = (get & path("metrics"))(metrics(registry))
```

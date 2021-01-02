# akka-http-metrics

[![Continuous Integration](https://github.com/RustedBones/akka-http-metrics/workflows/Continuous%20Integration/badge.svg?branch=master)](https://github.com/RustedBones/akka-http-metrics/actions?query=branch%3Amaster+workflow%3A"Continuous+Integration")
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/fr.davit/akka-http-metrics-core_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/fr.davit/akka-http-metrics-core_2.13)
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
| `1.4.1` | 2020-12-14   | `10.2.2`          | `2.13.4`, `2.12.12` |
| `1.4.0` | 2020-12-12   | `10.2.2`          | `2.13.4`, `2.12.12` |
| `1.3.0` | 2020-11-09   | `10.2.1`          | `2.13.3`, `2.12.12` |
| `1.2.0` | 2020-08-29   | `10.2.0`          | `2.13.3`, `2.12.12` |
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

- requests (`counter`) [method]
- requests active (`gauge`) [method]
- requests failures (`counter`) [method]
- requests size (`histogram`) [method]
- responses (`counter`) [method | path | status group]
- responses errors [method | path | status group]
- responses duration (`histogram`) [method | path | status group]
- response size (`histogram`) [method | path | status group]
- connections (`counter`)
- connections active (`gauge`)

Record metrics from your akka server by importing the implicits from `HttpMetricsRoute`. Convert your route to the
flow that will handle requests with `recordMetrics` and bind your server to the desired port.

```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import fr.davit.akka.http.metrics.core.{HttpMetricsRegistry, HttpMetricsSettings}
import fr.davit.akka.http.metrics.core.HttpMetrics._

implicit val system = ActorSystem()

val settings: HttpMetricsSettings = ... // concrete settings implementation

val registry: HttpMetricsRegistry = ... // concrete registry implementation

val route: Route = ... // your route

Http().newMeteredServerAt("localhost", 8080, registry).bindFlow(route)
```

Requests failure counter is incremented when no response could be emitted by the server (network error, ...)

By default, the response error counter will be incremented when the returned status code is an `Server error (5xx)`.
You can override this behaviour in the settings.

```scala
settings.withDefineError(_.status.isFailure)
```

In this example, all responses with status >= 400 are considered as errors.

For HTTP2 you must use the `bind` or `bindSync` on the `ServerBuilder`. The `Route` will be converted to
a `HttpRequest => HttpResponse` handler function. In this case the connection metrics won't be available.

```scala
Http().newMeteredServerAt("localhost", 8080).bind(route)
```

#### Labels

By default metrics labels are disabled. You can enable them in the settings.

```scala
settings
  .withIncludeMethodDimension(true)
  .withIncludePathDimension(true)
  .withIncludeStatusDimension(true)
```

You can also add additional static server-level dimensions to all metrics collected by the library. In the example
below, the `env` label with `prod` dimension will be added. 

```scala
import fr.davit.akka.http.metrics.core.Dimension

final case class EnvDimension(value: String) extends Dimension {
  override def key: String = "env"
}

settings.withServerDimensions(Seq(EnvDimension("prod")))
```

These key/value pairs will be added to all response size and response duration metrics.

It is up to the implementor to implement a class extending the `Dimension` trait.

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

val route = (get & path("metrics"))(metrics(registry))
```

Of course, you will also need to have the implicit marshaller for your registry in scope.


## Implementations

### [Datadog]( https://docs.datadoghq.com/developers/dogstatsd/)

| metric             | name                    |
|--------------------|-------------------------|
| requests           | requests_count          |
| requests active    | requests_active         |
| requests failures  | requests_failures_count |
| requests size      | requests_bytes          |
| responses          | responses_count         |
| responses errors   | responses_errors_count  |
| responses duration | responses_duration      |
| responses size     | responses_bytes         |
| connections        | connections_count       |
| connections active | connections_active      |

The `DatadogRegistry` is just a facade to publish to your StatsD server. The registry itself not located in the JVM, 
for this reason it is not possible to expose the metrics in your API.

Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-datadog" % <version>
```

Create your registry

```scala
import com.timgroup.statsd.StatsDClient
import fr.davit.akka.http.metrics.core.HttpMetricsSettings
import fr.davit.akka.http.metrics.datadog.{DatadogRegistry, DatadogSettings}

val client: StatsDClient = ... // your statsd client
val settings: HttpMetricsSettings = DatadogSettings.default
val registry = DatadogRegistry(client, settings) // or DatadogRegistry(client) to use default settings
```

See datadog's [documentation](https://github.com/dataDog/java-dogstatsd-client) on how to create a StatsD client.


### [Dropwizard](https://metrics.dropwizard.io/)

| metric             | name               |
|--------------------|--------------------|
| requests           | requests           |
| requests active    | requests.active    |
| requests failures  | requests.failures  |
| requests size      | requests.bytes     |
| responses          | responses          |
| responses errors   | responses.errors   |
| responses duration | responses.duration |
| responses size     | responses.bytes    |
| connections        | connections        |
| connections active | connections.active |

**Important**: The `DropwizardRegistry` does not support labels.
This feature will be available with dropwizard `v5`, which development is paused at the moment.

Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-dropwizard" % <version>
// or for dropwizard v5
libraryDependencies += "fr.davit" %% "akka-http-metrics-dropwizard-v5" % <version>
```

Create your registry

```scala
import com.codahale.metrics.MetricRegistry
import fr.davit.akka.http.metrics.core.HttpMetricsSettings
import fr.davit.akka.http.metrics.dropwizard.{DropwizardRegistry, DropwizardSettings}

val dropwizard: MetricRegistry = ... // your dropwizard registry
val settings: HttpMetricsSettings = DropwizardSettings.default
val registry = DropwizardRegistry(dropwizard, settings) // or DropwizardRegistry() to use a fresh registry & default settings
```

Expose the metrics

```scala
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._
import fr.davit.akka.http.metrics.dropwizard.marshalling.DropwizardMarshallers._

val route = (get & path("metrics"))(metrics(registry))
```

All metrics from the dropwizard metrics registry will be exposed.
You can find some external exporters [here](https://github.com/dropwizard/metrics/). For instance,
to expose some JVM metrics, you have to add the dedicated dependency and register the metrics set into your collector registry:

```sbt
libraryDependencies += "com.codahale.metrics" % "metrics-jvm" % <version>
```

```scala
import com.codahale.metrics.jvm._

val dropwizard: MetricRegistry = ... // your dropwizard registry
dropwizard.register("jvm.gc", new GarbageCollectorMetricSet())
dropwizard.register("jvm.threads", new CachedThreadStatesGaugeSet(10, TimeUnit.SECONDS))
dropwizard.register("jvm.memory", new MemoryUsageGaugeSet())

val registry = DropwizardRegistry(dropwizard, settings)
```

### [Graphite](https://graphiteapp.org/)

| metric             | name               |
|--------------------|--------------------|
| requests           | requests           |
| requests active    | requests.active    |
| requests failures  | requests.failures  |
| requests size      | requests.bytes     |
| responses          | responses          |
| responses errors   | responses.errors   |
| responses duration | responses.duration |
| response size      | responses.bytes    |
| connections        | connections        |
| connections active | connections.active |

Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-graphite" % <version>
```

Create your carbon client and your registry

```scala
import fr.davit.akka.http.metrics.core.HttpMetricsSettings
import fr.davit.akka.http.metrics.graphite.{CarbonClient, GraphiteRegistry, GraphiteSettings}

val carbonClient: CarbonClient = CarbonClient("hostname", 2003)
val settings: HttpMetricsSettings = GraphiteSettings.default
val registry = GraphiteRegistry(carbonClient, settings) // or PrometheusRegistry(carbonClient) to use default settings
```

### [Prometheus](http://prometheus.io/)

| metric             | name                       |
|--------------------|----------------------------|
| requests           | requests_total             |
| requests active    | requests_active            |
| requests failures  | requests_failures_total    |
| requests size      | requests_size_bytes        |
| responses          | responses_total            |
| responses errors   | responses_errors_total     |
| responses duration | responses_duration_seconds |
| responses size     | responses_size_bytes       |
| connections        | connections_total          |
| connections active | connections_active         |

Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-prometheus" % <version>
```

Create your registry

```scala
import io.prometheus.client.CollectorRegistry
import fr.davit.akka.http.metrics.prometheus.{PrometheusRegistry, PrometheusSettings}

val prometheus: CollectorRegistry = ... // your prometheus registry
val settings: PrometheusSettings = PrometheusSettings.default
val registry = PrometheusRegistry(prometheus, settings) // or PrometheusRegistry() to use the default registry & settings
```

You can fine-tune the `histogram/summary` configuration of `buckets/quantiles` for the `request
 size`, `duration` and `response size` metrics.
 
```scala
settings
  .withDurationConfig(Buckets(1, 2, 3, 5, 8, 13, 21, 34))
  .withReceivedBytesConfig(Quantiles(0.5, 0.75, 0.9, 0.95, 0.99))
  .withSentBytesConfig(PrometheusSettings.DefaultQuantiles)
```

Expose the metrics

```scala
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers._

val route = (get & path("metrics"))(metrics(registry))
```

All metrics from the prometheus collector registry will be exposed.
You can find some external exporters [here](https://github.com/prometheus/client_java). For instance, to expose some JVM
metrics, you have to add the dedicated client dependency and initialize/register it to your collector registry:

```sbt
libraryDependencies += "fr.davit" %% "akka-http-metrics-graphite" % <version>
```

```scala
import io.prometheus.client.hotspot.DefaultExports

val prometheus: CollectorRegistry = ... // your prometheus registry
DefaultExports.register(prometheus)  // or DefaultExports.initialize() to use the default registry
```
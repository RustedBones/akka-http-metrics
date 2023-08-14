# akka-http-metrics

[![Continuous Integration](https://github.com/RustedBones/akka-http-metrics/actions/workflows/ci.yml/badge.svg)](https://github.com/RustedBones/akka-http-metrics/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/fr.davit/akka-http-metrics-core_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/fr.davit/akka-http-metrics-core_2.13)
[![Software License](https://img.shields.io/badge/license-Apache%202-brightgreen.svg?style=flat)](LICENSE)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

Easily collect and expose metrics in your akka-http server.

After the akka [licencing change](https://www.lightbend.com/blog/why-we-are-changing-the-license-for-akka),
no further development is expected on `akka-http-metrics`.
If you're migrating to pekko-http, see [pekko-http-metrics](https://github.com/RustedBones/pekko-http-metrics).

The following implementations are supported:

* [datadog](#datadog) (via StatsD)
* [dropwizard](#dropwizard)
* [graphite](#graphite) (via Carbon)
* [prometheus](#prometheus)

## Versions

| Version | Release date | Akka Http version | Scala versions      |
|---------|--------------|-------------------|---------------------|
| `1.7.1` | 2022-06-07   | `10.2.9`          | `2.13.8`, `2.12.15` |
| `1.7.0` | 2022-04-11   | `10.2.9`          | `2.13.8`, `2.12.15` |
| `1.6.0` | 2021-05-07   | `10.2.4`          | `2.13.5`, `2.12.13` |
| `1.5.1` | 2021-02-16   | `10.2.3`          | `2.13.4`, `2.12.12` |
| `1.5.0` | 2021-01-12   | `10.2.2`          | `2.13.4`, `2.12.12` |

The complete list can be found in the [CHANGELOG](CHANGELOG.md) file.

## Getting akka-http-metrics

Libraries are published to Maven Central. Add to your `build.sbt`:

```scala
libraryDependencies += "fr.davit" %% "akka-http-metrics-<backend>" % <version>
```

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

Record metrics from your akka server by creating an `HttpMetricsServerBuilder` with the `newMeteredServerAt` extension
method located in `HttpMetrics`

```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import fr.davit.akka.http.metrics.core.{HttpMetricsRegistry, HttpMetricsSettings}
import fr.davit.akka.http.metrics.core.HttpMetrics._ // import extension methods

implicit val system = ActorSystem()

val settings: HttpMetricsSettings = ... // concrete settings implementation

val registry: HttpMetricsRegistry = ... // concrete registry implementation

val route: Route = ... // your route

Http()
  .newMeteredServerAt("localhost", 8080, registry)
  .bindFlow(route)
```

Requests failure counter is incremented when no response could be emitted by the server (network error, ...)

By default, the response error counter will be incremented when the returned status code is an `Server error (5xx)`.
You can override this behaviour in the settings.

```scala
settings.withDefineError(_.status.isFailure)
```

In this example, all responses with status >= 400 are considered as errors.

For HTTP2 you must use the `bind` or `bindSync` on the `HttpMetricsServerBuilder`. 
In this case the connection metrics won't be available.

```scala
Http()
  .newMeteredServerAt("localhost", 8080)
  .bind(route)
```

#### Dimensions

By default, metrics dimensions are disabled. You can enable them in the settings.

```scala
settings
  .withIncludeMethodDimension(true)
  .withIncludePathDimension(true)
  .withIncludeStatusDimension(true)
```

Custom dimensions can be added to the message metrics:
- extend the `HttpRequestLabeler` to add labels on requests & their associated response 
- extend the `HttpResponseLabeler` to add labels on responses only

In the example below, the `browser` dimension will be populated based on the user-agent header on requests and responses.
The responses going through the route will have the `user` dimension set with the provided username, other responses
will be `unlabelled`.

```scala
import fr.davit.akka.http.metrics.core.{AttributeLabeler, HttpRequestLabeler}

// based on https://developer.mozilla.org/en-US/docs/Web/HTTP/Browser_detection_using_the_user_agent#browser_name
object BrowserLabeler extends HttpRequestLabeler {
 override def name: String = "browser"
 override def label(request: HttpRequest): String = {
  val products = for {
   ua <- request.header[`User-Agent`].toSeq
   pv <- ua.products
  } yield pv.product
  if (products.contains("Seamonkey")) "seamonkey"
  else if (products.contains("Firefox")) "firefox"
  else if (products.contains("Chromium")) "chromium"
  else if (products.contains("Chrome")) "chrome"
  else if (products.contains("Safari")) "safari"
  else if (products.contains("OPR") || products.contains("Opera")) "opera"
  else "other"
 }
}

object UserLabeler extends AttributeLabeler {
  def name: String = "user"
}

val route = auth { username =>
 metricsLabeled(UserLabeler, username) {
  ...
 }
}

settings.withCustomDimensions(BrowserLabeler, UserLabeler)
```


Additional static server-level dimensions can be set to all metrics collected by the library.
In the example below, the `env` dimension with `prod` label will be added. 

```scala
import fr.davit.akka.http.metrics.core.Dimension
settings.withServerDimensions(Dimension("env", "prod"))
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
libraryDependencies += "io.prometheus" % "simpleclient_hotspot" % <vesion>
```

```scala
import io.prometheus.client.hotspot.DefaultExports

val prometheus: CollectorRegistry = ... // your prometheus registry
DefaultExports.register(prometheus)  // or DefaultExports.initialize() to use the default registry
```

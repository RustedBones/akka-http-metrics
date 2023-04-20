import sbt._

object Dependencies {

  object Versions {
    val Pekko                 = "0.0.0+26633-3a8d86f8-SNAPSHOT"
    val PekkoHttp             = "0.0.0+4340-878dd99f-SNAPSHOT"
    val Datadog               = "4.1.0"
    val Dropwizard            = "4.2.14"
    val DropwizardV5          = "5.0.0"
    val Enumeratum            = "1.7.0"
    val Logback               = "1.2.11"
    val Prometheus            = "0.16.0"
    val ScalaCollectionCompat = "2.9.0"
    val ScalaLogging          = "3.9.4"
    val ScalaMock             = "5.2.0"
    val ScalaTest             = "3.2.14"
  }

  val PekkoHttp        = "org.apache.pekko"           %% "pekko-http"             % Versions.PekkoHttp
  val Datadog          = "com.datadoghq"               % "java-dogstatsd-client" % Versions.Datadog
  val DropwizardCore   = "io.dropwizard.metrics"       % "metrics-core"          % Versions.Dropwizard
  val DropwizardJson   = "io.dropwizard.metrics"       % "metrics-json"          % Versions.Dropwizard
  val DropwizardV5Core = "io.dropwizard.metrics5"      % "metrics-core"          % Versions.DropwizardV5
  val DropwizardV5Json = "io.dropwizard.metrics5"      % "metrics-json"          % Versions.DropwizardV5
  val Enumeratum       = "com.beachape"               %% "enumeratum"            % Versions.Enumeratum
  val PrometheusCommon = "io.prometheus"               % "simpleclient_common"   % Versions.Prometheus
  val ScalaLogging     = "com.typesafe.scala-logging" %% "scala-logging"         % Versions.ScalaLogging

  object Provided {
    val PekkoStream = "org.apache.pekko" %% "pekko-stream" % Versions.Pekko % "provided"
  }

  object Test {
    val PekkoHttpJson = "org.apache.pekko" %% "pekko-http-spray-json" % Versions.PekkoHttp % "it,test"
    val PekkoHttpTestkit = "org.apache.pekko" %% "pekko-http-testkit" % Versions.PekkoHttp % "it,test"
    val PekkoSlf4j = "org.apache.pekko" %% "pekko-slf4j" % Versions.Pekko % "it,test"
    val PekkoStreamTestkit = "org.apache.pekko" %% "pekko-stream-testkit" % Versions.Pekko % "it,test"
    val PekkoTestkit = "org.apache.pekko" %% "pekko-testkit" % Versions.Pekko % "it,test"

    val DropwizardJvm     = "io.dropwizard.metrics"  % "metrics-jvm"          % Versions.Dropwizard   % "it,test"
    val DropwizardV5Jvm   = "io.dropwizard.metrics5" % "metrics-jvm"          % Versions.DropwizardV5 % "it,test"
    val Logback           = "ch.qos.logback"         % "logback-classic"      % Versions.Logback      % "it,test"
    val PrometheusHotspot = "io.prometheus"          % "simpleclient_hotspot" % Versions.Prometheus   % "it,test"

    val ScalaCollectionCompat =
      "org.scala-lang.modules" %% "scala-collection-compat" % Versions.ScalaCollectionCompat % "it,test"
    val ScalaMock = "org.scalamock" %% "scalamock" % Versions.ScalaMock % "it,test"
    val ScalaTest = "org.scalatest" %% "scalatest" % Versions.ScalaTest % "it,test"
  }
}

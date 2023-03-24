import sbt._

object Dependencies {

  object Versions {
    val Akka                  = "2.6.20"
    val AkkaHttp              = "10.2.10"
    val Datadog               = "4.1.0"
    val Dropwizard            = "4.2.14"
    val DropwizardV5          = "5.0.0"
    val Enumeratum            = "1.7.0"
    val Logback               = "1.2.12"
    val Prometheus            = "0.16.0"
    val ScalaCollectionCompat = "2.9.0"
    val ScalaLogging          = "3.9.4"
    val ScalaMock             = "5.2.0"
    val ScalaTest             = "3.2.14"
  }

  val AkkaHttp         = "com.typesafe.akka"          %% "akka-http"             % Versions.AkkaHttp
  val Datadog          = "com.datadoghq"               % "java-dogstatsd-client" % Versions.Datadog
  val DropwizardCore   = "io.dropwizard.metrics"       % "metrics-core"          % Versions.Dropwizard
  val DropwizardJson   = "io.dropwizard.metrics"       % "metrics-json"          % Versions.Dropwizard
  val DropwizardV5Core = "io.dropwizard.metrics5"      % "metrics-core"          % Versions.DropwizardV5
  val DropwizardV5Json = "io.dropwizard.metrics5"      % "metrics-json"          % Versions.DropwizardV5
  val Enumeratum       = "com.beachape"               %% "enumeratum"            % Versions.Enumeratum
  val PrometheusCommon = "io.prometheus"               % "simpleclient_common"   % Versions.Prometheus
  val ScalaLogging     = "com.typesafe.scala-logging" %% "scala-logging"         % Versions.ScalaLogging

  object Provided {
    val AkkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.Akka % "provided"
  }

  object Test {
    val AkkaHttpJson      = "com.typesafe.akka"     %% "akka-http-spray-json" % Versions.AkkaHttp     % "it,test"
    val AkkaHttpTestkit   = "com.typesafe.akka"     %% "akka-http-testkit"    % Versions.AkkaHttp     % "it,test"
    val AkkaSlf4j         = "com.typesafe.akka"     %% "akka-slf4j"           % Versions.Akka         % "it,test"
    val AkkaStreamTestkit = "com.typesafe.akka"     %% "akka-stream-testkit"  % Versions.Akka         % "it,test"
    val AkkaTestkit       = "com.typesafe.akka"     %% "akka-testkit"         % Versions.Akka         % "it,test"
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

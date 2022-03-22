import sbt._

object Dependencies {

  object Versions {
    val akka                  = "2.6.18"
    val akkaHttp              = "10.2.7"
    val datadog               = "4.0.0"
    val dropwizard            = "4.2.7"
    val dropwizardV5          = "5.0.0"
    val enumeratum            = "1.7.0"
    val logback               = "1.2.10"
    val prometheus            = "0.14.1"
    val scalaCollectionCompat = "2.7.0"
    val scalaLogging          = "3.9.4"
    val scalaMock             = "5.2.0"
    val scalaTest             = "3.2.11"
  }

  val akkaHttp         = "com.typesafe.akka"          %% "akka-http"             % Versions.akkaHttp
  val datadog          = "com.datadoghq"               % "java-dogstatsd-client" % Versions.datadog
  val dropwizardCore   = "io.dropwizard.metrics"       % "metrics-core"          % Versions.dropwizard
  val dropwizardJson   = "io.dropwizard.metrics"       % "metrics-json"          % Versions.dropwizard
  val dropwizardV5Core = "io.dropwizard.metrics5"      % "metrics-core"          % Versions.dropwizardV5
  val dropwizardV5Json = "io.dropwizard.metrics5"      % "metrics-json"          % Versions.dropwizardV5
  val enumeratum       = "com.beachape"               %% "enumeratum"            % Versions.enumeratum
  val prometheusCommon = "io.prometheus"               % "simpleclient_common"   % Versions.prometheus
  val scalaLogging     = "com.typesafe.scala-logging" %% "scala-logging"         % Versions.scalaLogging

  object Provided {
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.akka % "provided"
  }

  object Test {
    val akkaHttpJson      = "com.typesafe.akka"     %% "akka-http-spray-json" % Versions.akkaHttp     % "it,test"
    val akkaHttpTestkit   = "com.typesafe.akka"     %% "akka-http-testkit"    % Versions.akkaHttp     % "it,test"
    val akkaSlf4j         = "com.typesafe.akka"     %% "akka-slf4j"           % Versions.akka         % "it,test"
    val akkaStreamTestkit = "com.typesafe.akka"     %% "akka-stream-testkit"  % Versions.akka         % "it,test"
    val akkaTestkit       = "com.typesafe.akka"     %% "akka-testkit"         % Versions.akka         % "it,test"
    val dropwizardJvm     = "io.dropwizard.metrics"  % "metrics-jvm"          % Versions.dropwizard   % "it,test"
    val dropwizardV5Jvm   = "io.dropwizard.metrics5" % "metrics-jvm"          % Versions.dropwizardV5 % "it,test"
    val logback           = "ch.qos.logback"         % "logback-classic"      % Versions.logback      % "it,test"
    val prometheusHotspot = "io.prometheus"          % "simpleclient_hotspot" % Versions.prometheus   % "it,test"

    val scalaCollectionCompat =
      "org.scala-lang.modules" %% "scala-collection-compat" % Versions.scalaCollectionCompat % "it,test"
    val scalaMock = "org.scalamock" %% "scalamock" % Versions.scalaMock % "it,test"
    val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalaTest % "it,test"
  }

}

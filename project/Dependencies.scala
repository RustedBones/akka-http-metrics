import sbt._

object Dependencies {

  object Versions {
    val akka       = "2.5.19"
    val akkaHttp   = "10.1.6"
    val datadog    = "2.6.1"
    val dropwizard = "4.0.3"
    val logback    = "1.2.3"
    val prometheus = "0.6.0"
    val scalaMock  = "4.1.0"
    val scalaTest  = "3.0.5"
  }

  val akkaHttp             = "com.typesafe.akka"     %% "akka-http"              % Versions.akkaHttp
  val datadog              = "com.datadoghq"         % "java-dogstatsd-client"   % Versions.datadog
  val dropwizardCore       = "io.dropwizard.metrics" % "metrics-core"            % Versions.dropwizard
  val dropwizardJson       = "io.dropwizard.metrics" % "metrics-json"            % Versions.dropwizard
  val prometheusCommon     = "io.prometheus"         % "simpleclient_common"     % Versions.prometheus
  val prometheusDropwizard = "io.prometheus"         % "simpleclient_dropwizard" % Versions.prometheus

  object Provided {
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.akka % "provided"
  }

  object Test {
    val akkaHttpJson      = "com.typesafe.akka" %% "akka-http-spray-json" % Versions.akkaHttp  % "test"
    val akkaHttpTestkit   = "com.typesafe.akka" %% "akka-http-testkit"    % Versions.akkaHttp  % "test"
    val akkaSlf4j         = "com.typesafe.akka" %% "akka-slf4j"           % Versions.akka      % "test"
    val akkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit"  % Versions.akka      % "test"
    val akkaTestkit       = "com.typesafe.akka" %% "akka-testkit"         % Versions.akka      % "test"
    val logback           = "ch.qos.logback"    % "logback-classic"       % Versions.logback   % "test"
    val scalaMock         = "org.scalamock"     %% "scalamock"            % Versions.scalaMock % "test"
    val scalaTest         = "org.scalatest"     %% "scalatest"            % Versions.scalaTest % "test"
  }

}

import sbt._

object Dependencies {

  object Versions {
    val akka       = "2.6.8"
    val akkaHttp   = "10.2.0"
    val datadog    = "2.10.1"
    val dropwizard = "5.0.0"
    val enumeratum = "1.6.1"
    val logback    = "1.2.3"
    val prometheus = "0.9.0"
    val scalaMock  = "4.4.0"
    val scalaTest  = "3.1.2"
  }

  val akkaHttp             = "com.typesafe.akka"      %% "akka-http"              % Versions.akkaHttp
  val datadog              = "com.datadoghq"          % "java-dogstatsd-client"   % Versions.datadog
  val dropwizardCore       = "io.dropwizard.metrics5" % "metrics-core"            % Versions.dropwizard
  val dropwizardJson       = "io.dropwizard.metrics5" % "metrics-json"            % Versions.dropwizard
  val enumeratum           = "com.beachape"           %% "enumeratum"             % Versions.enumeratum
  val prometheusCommon     = "io.prometheus"          % "simpleclient_common"     % Versions.prometheus
  val prometheusDropwizard = "io.prometheus"          % "simpleclient_dropwizard" % Versions.prometheus

  object Provided {
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.akka % "provided"
  }

  object Test {
    val akkaHttpJson      = "com.typesafe.akka" %% "akka-http-spray-json" % Versions.akkaHttp  % "it,test"
    val akkaHttpTestkit   = "com.typesafe.akka" %% "akka-http-testkit"    % Versions.akkaHttp  % "it,test"
    val akkaSlf4j         = "com.typesafe.akka" %% "akka-slf4j"           % Versions.akka      % "it,test"
    val akkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit"  % Versions.akka      % "it,test"
    val akkaTestkit       = "com.typesafe.akka" %% "akka-testkit"         % Versions.akka      % "it,test"
    val logback           = "ch.qos.logback"    % "logback-classic"       % Versions.logback   % "it,test"
    val scalaMock         = "org.scalamock"     %% "scalamock"            % Versions.scalaMock % "it,test"
    val scalaTest         = "org.scalatest"     %% "scalatest"            % Versions.scalaTest % "it,test"
  }

}

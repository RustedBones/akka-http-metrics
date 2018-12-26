import sbt._

object Dependencies {

  object Versions {
    val akkaHttp   = "10.1.6"
    val akkaStream = "2.5.19"
    val datadog    = "2.6.1"
    val dropwizard = "4.0.3"
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
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.akkaStream % "provided"
  }

  object Test {
    val akkaHttpJson    = "com.typesafe.akka" %% "akka-http-spray-json" % Versions.akkaHttp
    val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit"    % Versions.akkaHttp % "test"
    val scalaMock       = "org.scalamock"     %% "scalamock"            % Versions.scalaMock % "test"
    val scalaTest       = "org.scalatest"     %% "scalatest"            % Versions.scalaTest % "test"
  }

}

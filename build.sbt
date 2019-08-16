// General info
val username = "RustedBones"
val repo = "akka-http-metrics"

lazy val commonSettings = Defaults.itSettings ++ Seq(
  organization := "fr.davit",
  version := "0.5.1-SNAPSHOT",
  crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0"),
  scalaVersion := crossScalaVersions.value.last,
  Compile / compile / scalacOptions ++= Settings.scalacOptions(scalaVersion.value),

  homepage := Some(url(s"https://github.com/$username/$repo")),
  licenses += "APACHE" -> url(s"https://github.com/$username/$repo/blob/master/LICENSE"),
  scmInfo := Some(ScmInfo(url(s"https://github.com/$username/$repo"), s"git@github.com:$username/$repo.git")),
  developers := List(
    Developer(id = s"$username", name = "Michel Davit", email = "michel@davit.fr", url = url(s"https://github.com/$username"))
  ),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
)

lazy val `akka-http-metrics` = (project in file("."))
  .aggregate(
    `akka-http-metrics-core`,
    `akka-http-metrics-datadog`,
    `akka-http-metrics-graphite`,
    `akka-http-metrics-dropwizard`,
    `akka-http-metrics-prometheus`
  )
  .settings(commonSettings: _*)
  .settings(
    publishArtifact := false
  )

lazy val `akka-http-metrics-core` = (project in file("akka-http-metrics-core"))
  .configs(IntegrationTest)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.akkaHttp,
      Dependencies.enumeratum,
      Dependencies.Provided.akkaStream,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.akkaSlf4j,
      Dependencies.Test.akkaStreamTestkit,
      Dependencies.Test.logback,
      Dependencies.Test.scalaMock,
      Dependencies.Test.scalaTest
    ),
  )

lazy val `akka-http-metrics-datadog` = (project in file("akka-http-metrics-datadog"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.datadog,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.akkaStreamTestkit,
      Dependencies.Test.akkaSlf4j,
      Dependencies.Test.logback,
      Dependencies.Test.scalaTest
    ),
  )

lazy val `akka-http-metrics-dropwizard` = (project in file("akka-http-metrics-dorpwizard"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.dropwizardCore,
      Dependencies.dropwizardJson,
      Dependencies.Provided.akkaStream,
      Dependencies.Test.akkaHttpJson,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.akkaSlf4j,
      Dependencies.Test.akkaTestkit,
      Dependencies.Test.logback,
      Dependencies.Test.scalaTest
    )
  )

lazy val `akka-http-metrics-graphite` = (project in file("akka-http-metrics-graphite"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Provided.akkaStream,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.akkaStreamTestkit,
      Dependencies.Test.akkaSlf4j,
      Dependencies.Test.logback,
      Dependencies.Test.scalaTest
    ),
  )

lazy val `akka-http-metrics-prometheus` = (project in file("akka-http-metrics-prometheus"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.prometheusCommon,
      Dependencies.prometheusDropwizard,
      Dependencies.Provided.akkaStream,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.akkaSlf4j,
      Dependencies.Test.akkaTestkit,
      Dependencies.Test.logback,
      Dependencies.Test.scalaTest
    )
  )

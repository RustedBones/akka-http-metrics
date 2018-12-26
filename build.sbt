// General info
val username = "RustedBones"
val repo = "akka-http-metrics"

lazy val commonSettings = Seq(
  organization := "fr.davit",
  version := "0.2.0-SNAPSHOT",
  crossScalaVersions := Seq("2.11.13", "2.12.8"),
  scalaVersion := (ThisBuild / crossScalaVersions).value.last,
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

// akka-http-prometheus
lazy val `akka-http-metrics` = (project in file("."))
  .aggregate(`akka-http-metrics-core`, `akka-http-metrics-datadog`, `akka-http-metrics-dropwizard`, `akka-http-metrics-prometheus`)
  .settings(commonSettings: _*)

lazy val `akka-http-metrics-core` = (project in file("akka-http-metrics-core"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.akkaHttp,
      Dependencies.Provided.akkaStream,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.scalaMock,
      Dependencies.Test.scalaTest
    ),
  )

lazy val `akka-http-metrics-datadog` = (project in file("akka-http-metrics-datadog"))
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.datadog,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.scalaTest
    ),
  )

lazy val `akka-http-metrics-dropwizard` = (project in file("akka-http-metrics-dorpwizard"))
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.dropwizardCore,
      Dependencies.dropwizardJson,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.akkaHttpJson,
      Dependencies.Test.scalaTest
    )
  )

lazy val `akka-http-metrics-prometheus` = (project in file("akka-http-metrics-prometheus"))
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.prometheusCommon,
      Dependencies.prometheusDropwizard,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.scalaTest
    )
  )

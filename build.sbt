// General info
val username = "RustedBones"
val repo     = "akka-http-metrics"

lazy val filterScalacOptions = { options: Seq[String] =>
  options.filterNot { o =>
    // get rid of value discard
    o == "-Ywarn-value-discard" || o == "-Wvalue-discard"
  }
}

// for sbt-github-actions
ThisBuild / crossScalaVersions := Seq("2.13.4", "2.12.12")
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(name = Some("Check project"), commands = List("scalafmtCheckAll", "headerCheckAll")),
  WorkflowStep.Sbt( name = Some("Build project"), commands = List("test", "it:test"))
)
ThisBuild / githubWorkflowTargetBranches := Seq("master")
ThisBuild / githubWorkflowPublishTargetBranches := Seq.empty

lazy val commonSettings = Defaults.itSettings ++
    headerSettings(IntegrationTest) ++
    Seq(
      organization := "fr.davit",
      organizationName := "Michel Davit",
      version := "1.5.0",
      crossScalaVersions := (ThisBuild / crossScalaVersions).value,
      scalaVersion := crossScalaVersions.value.head,
      scalacOptions ~= filterScalacOptions,
      homepage := Some(url(s"https://github.com/$username/$repo")),
      licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
      startYear := Some(2019),
      scmInfo := Some(ScmInfo(url(s"https://github.com/$username/$repo"), s"git@github.com:$username/$repo.git")),
      developers := List(
        Developer(
          id = s"$username",
          name = "Michel Davit",
          email = "michel@davit.fr",
          url = url(s"https://github.com/$username")
        )
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
    `akka-http-metrics-dropwizard-v5`,
    `akka-http-metrics-prometheus`
  )
  .settings(commonSettings: _*)
  .settings(
    publishArtifact := false
  )

lazy val `akka-http-metrics-core` = (project in file("core"))
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
    )
  )

lazy val `akka-http-metrics-datadog` = (project in file("datadog"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.datadog,
      Dependencies.Provided.akkaStream,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.akkaSlf4j,
      Dependencies.Test.akkaStreamTestkit,
      Dependencies.Test.logback,
      Dependencies.Test.scalaTest
    )
  )

lazy val `akka-http-metrics-dropwizard` = (project in file("dropwizard"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.dropwizardCore,
      Dependencies.dropwizardJson,
      Dependencies.scalaLogging,
      Dependencies.Provided.akkaStream,
      Dependencies.Test.akkaHttpJson,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.akkaSlf4j,
      Dependencies.Test.akkaStreamTestkit,
      Dependencies.Test.akkaTestkit,
      Dependencies.Test.dropwizardJvm,
      Dependencies.Test.logback,
      Dependencies.Test.scalaCollectionCompat,
      Dependencies.Test.scalaTest
    )
  )

lazy val `akka-http-metrics-dropwizard-v5` = (project in file("dropwizard-v5"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.dropwizardV5Core,
      Dependencies.dropwizardV5Json,
      Dependencies.Provided.akkaStream,
      Dependencies.Test.akkaHttpJson,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.akkaSlf4j,
      Dependencies.Test.akkaStreamTestkit,
      Dependencies.Test.akkaTestkit,
      Dependencies.Test.dropwizardV5Jvm,
      Dependencies.Test.logback,
      Dependencies.Test.scalaCollectionCompat,
      Dependencies.Test.scalaTest
    )
  )

lazy val `akka-http-metrics-graphite` = (project in file("graphite"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Provided.akkaStream,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.akkaSlf4j,
      Dependencies.Test.akkaStreamTestkit,
      Dependencies.Test.logback,
      Dependencies.Test.scalaTest
    )
  )

lazy val `akka-http-metrics-prometheus` = (project in file("prometheus"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.prometheusCommon,
      Dependencies.Provided.akkaStream,
      Dependencies.Test.akkaHttpTestkit,
      Dependencies.Test.akkaSlf4j,
      Dependencies.Test.akkaStreamTestkit,
      Dependencies.Test.akkaTestkit,
      Dependencies.Test.logback,
      Dependencies.Test.prometheusHotspot,
      Dependencies.Test.scalaTest
    )
  )

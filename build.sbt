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
ThisBuild / crossScalaVersions := Seq("2.13.10", "2.12.17")
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(name = Some("Check project"), commands = List("scalafmtCheckAll", "headerCheckAll")),
  WorkflowStep.Sbt(name = Some("Build project"), commands = List("test", "it:test"))
)
ThisBuild / githubWorkflowTargetBranches := Seq("main")
ThisBuild / githubWorkflowPublishTargetBranches := Seq.empty

lazy val commonSettings = Defaults.itSettings ++
  headerSettings(IntegrationTest) ++
  Seq(
    organization := "fr.davit",
    organizationName := "Michel Davit",
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
    // Release version of Pekko not yet available so use Apache nightlies for now
    resolvers += "Apache Nightlies" at "https://repository.apache.org/content/groups/snapshots",
    publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
    releaseCrossBuild := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
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
      Dependencies.AkkaHttp,
      Dependencies.PekkoHttp,
      Dependencies.Enumeratum,
      Dependencies.Provided.AkkaStream,
      Dependencies.Provided.PekkoStream,
      Dependencies.Test.AkkaHttpTestkit,
      Dependencies.Test.AkkaSlf4j,
      Dependencies.Test.AkkaStreamTestkit,
      Dependencies.Test.PekkoHttpTestkit,
      Dependencies.Test.PekkoSlf4j,Dependencies.Test.Logback,
      Dependencies.Test.PekkoStreamTestkit,Dependencies.Test.ScalaMock,
      Dependencies.Test.ScalaTest
    )
  )

lazy val `akka-http-metrics-datadog` = (project in file("datadog"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Datadog,
      Dependencies.Provided.AkkaStream,
      Dependencies.Provided.PekkoStream,
      Dependencies.Test.AkkaHttpTestkit,
      Dependencies.Test.AkkaSlf4j,
      Dependencies.Test.AkkaStreamTestkit,
      Dependencies.Test.PekkoHttpTestkit,
      Dependencies.Test.PekkoSlf4j,
      Dependencies.Test.PekkoStreamTestkit,
      Dependencies.Test.Logback,
      Dependencies.Test.ScalaTest
    )
  )

lazy val `akka-http-metrics-dropwizard` = (project in file("dropwizard"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.DropwizardCore,
      Dependencies.DropwizardJson,
      Dependencies.ScalaLogging,
      Dependencies.Provided.AkkaStream,
      Dependencies.Test.AkkaHttpJson,
      Dependencies.Test.AkkaHttpTestkit,
      Dependencies.Test.AkkaSlf4j,
      Dependencies.Test.AkkaStreamTestkit,
      Dependencies.Test.AkkaTestkit,

      Dependencies.Provided.PekkoStream,
      Dependencies.Test.PekkoHttpJson,
      Dependencies.Test.PekkoHttpTestkit,
      Dependencies.Test.PekkoSlf4j,
      Dependencies.Test.PekkoStreamTestkit,
      Dependencies.Test.PekkoTestkit,

      Dependencies.Test.DropwizardJvm,
      Dependencies.Test.Logback,
      Dependencies.Test.ScalaCollectionCompat,
      Dependencies.Test.ScalaTest
    )
  )

lazy val `akka-http-metrics-dropwizard-v5` = (project in file("dropwizard-v5"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.DropwizardV5Core,
      Dependencies.DropwizardV5Json,
      Dependencies.Provided.AkkaStream,
      Dependencies.Test.AkkaHttpJson,
      Dependencies.Test.AkkaHttpTestkit,
      Dependencies.Test.AkkaSlf4j,
      Dependencies.Test.AkkaStreamTestkit,
      Dependencies.Test.AkkaTestkit,

      Dependencies.Provided.PekkoStream,
      Dependencies.Test.PekkoHttpJson,
      Dependencies.Test.PekkoHttpTestkit,
      Dependencies.Test.PekkoSlf4j,
      Dependencies.Test.PekkoStreamTestkit,
      Dependencies.Test.PekkoTestkit,

      Dependencies.Test.DropwizardV5Jvm,
      Dependencies.Test.Logback,
      Dependencies.Test.ScalaCollectionCompat,
      Dependencies.Test.ScalaTest
    )
  )

lazy val `akka-http-metrics-graphite` = (project in file("graphite"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Provided.AkkaStream,
      Dependencies.Test.AkkaHttpTestkit,
      Dependencies.Test.AkkaSlf4j,
      Dependencies.Test.AkkaStreamTestkit,

      Dependencies.Provided.PekkoStream,
      Dependencies.Test.PekkoHttpTestkit,
      Dependencies.Test.PekkoSlf4j,
      Dependencies.Test.PekkoStreamTestkit,

      Dependencies.Test.Logback,
      Dependencies.Test.ScalaTest
    )
  )

lazy val `akka-http-metrics-prometheus` = (project in file("prometheus"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.PrometheusCommon,
      Dependencies.Provided.AkkaStream,
      Dependencies.Test.AkkaHttpTestkit,
      Dependencies.Test.AkkaSlf4j,
      Dependencies.Test.AkkaStreamTestkit,
      Dependencies.Test.AkkaTestkit,

      Dependencies.Provided.PekkoStream,
      Dependencies.Test.PekkoHttpTestkit,
      Dependencies.Test.PekkoSlf4j,
      Dependencies.Test.PekkoStreamTestkit,
      Dependencies.Test.PekkoTestkit,

      Dependencies.Test.Logback,
      Dependencies.Test.PrometheusHotspot,
      Dependencies.Test.ScalaTest
    )
  )

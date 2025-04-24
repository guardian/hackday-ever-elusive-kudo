ThisBuild / organization := "com.adamnfish"
ThisBuild / scalaVersion := "3.6.4"
scalacOptions ++= Seq(
  // format: off
  "-deprecation",
  "-encoding", "utf-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-source", "3.7"
  // format: on
)

val catsEffectVersion = "3.6.1"
val http4sVersion = "0.23.30"
val fs2Version = "3.12.0"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "eek",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.typelevel" %% "cats-effect-kernel" % catsEffectVersion,
      "org.typelevel" %% "cats-effect-std" % catsEffectVersion,
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "com.47deg" %% "github4s" % "0.33.3",
      "com.github.scopt" %% "scopt" % "4.1.0",
      "com.lihaoyi" %% "fastparse" % "3.1.1",
      "software.amazon.awssdk" % "bedrockruntime" % "2.31.28",
      "ch.qos.logback" % "logback-classic" % "1.5.18",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "org.slf4j" % "slf4j-api" % "2.0.17",
      "org.scalameta" %% "munit" % "1.1.0" % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.1.0" % Test,
      "org.scalameta" %% "munit-scalacheck" % "1.1.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    ),
    Compile / run / fork := true
  )

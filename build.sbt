ThisBuild / organization := "com.adamnfish"
ThisBuild / scalaVersion := "3.4.0"
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "utf-8",
  // "-explaintypes",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings"
)

val catsEffectVersion = "3.5.6"
val http4sVersion = "0.23.29"

lazy val root = (project in file("."))
  .settings(
    name := "tdc",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.typelevel" %% "cats-effect-kernel" % catsEffectVersion,
      "org.typelevel" %% "cats-effect-std" % catsEffectVersion,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "com.47deg" %% "github4s" % "0.33.3",
      "com.lihaoyi" %% "fastparse" % "3.1.1",
      "software.amazon.awssdk" % "bedrockruntime" % "2.29.22",
      "com.github.alexarchambault" %% "case-app-cats" % "2.1.0-M29",
      "ch.qos.logback" % "logback-classic" % "1.5.12",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "org.slf4j" % "slf4j-api" % "2.0.16",
      "org.scalameta" %% "munit" % "1.0.2" % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test,
      "org.scalameta" %% "munit-scalacheck" % "1.0.0" % Test
    ),
    Compile / run / fork := true
  )

ThisBuild / name := "cats-bot"
ThisBuild / organization := "bot"
ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.13.1"

lazy val root = (project in file("."))
  .aggregate(refined, core)

lazy val refined =
  (project in file("refined")).settings(name := "refined", settings, libraryDependencies ++= commonDependencies)
lazy val core = (project in file("core"))
  .settings(name := "core", settings, libraryDependencies ++= commonDependencies)
  .dependsOn(refined)

lazy val commonDependencies = Seq(
  "com.github.JavaBWAPI"        % "JBWAPI"          % "1.6" from "file:/home/drd/Documents/Repositories/JBWAPI/target/jbwapi-1.5.1-jar-with-dependencies.jar",
  "org.typelevel"              %% "cats-core"       % "2.0.0",
  "org.slf4j"                   % "slf4j-api"       % "1.7.25",
  "ch.qos.logback"              % "logback-classic" % "1.2.3",
  "io.monix"                   %% "monix"           % "3.2.2",
  "io.chrisdavenport"          %% "log4cats-slf4j"  % "1.1.1",
  "org.scalatest"              %% "scalatest"       % "3.0.8" % Test,
  "org.scalamock"              %% "scalamock"       % "5.0.0" % Test,
  "com.github.julien-truffaut" %% "monocle-core"    % "2.0.3",
  "com.github.julien-truffaut" %% "monocle-macro"   % "2.0.3",
  "org.apache.commons"          % "commons-math3"   % "3.6.1",
  "eu.timepit"                 %% "refined"         % "0.9.15",
  "eu.timepit"                 %% "refined-cats"    % "0.9.15",
  "eu.timepit"                 %% "refined-eval"    % "0.9.15",
  "org.typelevel"              %% "mouse"           % "0.25"
)

/*
lazy val dependencies = new {
  val javaBWAPIV = "1.5.1"

  val javaBWAPI =
    "com.github.JavaBWAPI" % "JBWAPI" % javaBWAPIV

}
 */

lazy val settings = commonSettings

lazy val compilerOptions = Seq("-language:higherKinds", "-Ymacro-annotations")

lazy val commonSettings = Seq(scalacOptions ++= compilerOptions, resolvers ++= Seq("jitPack" at "https://jitpack.io"))

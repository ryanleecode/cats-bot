name := "cats-bot"
version := "0.1"

scalaVersion := "2.13.1"

resolvers ++= Seq(
  "jitPack" at "https://jitpack.io",
)

libraryDependencies ++= Seq(
  "com.github.JavaBWAPI" % "JBWAPI" % "1.5" from "file:/home/drd/Documents/Repositories/JBWAPI/target/jbwapi-1.5-jar-with-dependencies.jar",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "io.monix" %% "monix" % "3.2.2",
  "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1"
)

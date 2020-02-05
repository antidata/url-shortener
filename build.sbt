import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

val akkaVersion = "2.5.4"

lazy val `url-shortener` = project
  .in(file("."))
  .settings(multiJvmSettings: _*)
  .settings(
    name := "UrlShortener",
    version := "1.0.1",
    organization := "qai.urlshortener",
    scalaVersion := "2.12.2",
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.1.0",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
      "net.liftweb" %% "lift-json" % "3.4.1",
      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      "io.kamon" % "sigar-loader" % "1.6.6-rev002"),
    fork in run := true,
    mainClass in (Compile, run) := Some("qai.urlshortener.UrlShortenerApp"),
    // disable parallel tests
    parallelExecution in Test := false,
    scriptClasspath +="../conf",
    bashScriptTemplateLocation := new File("src/main/resources/bash-template")
  )
  .configs (MultiJvm)
  .enablePlugins(JavaAppPackaging)

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name             := "harvestr",
    idePackagePrefix := Some("io.rudder")
  )

libraryDependencies ++= List(
  "dev.zio"              %% "zio"          % "2.1.1",
  "dev.zio"              %% "zio-json"     % "0.6.2",
  "com.github.pathikrit" %% "better-files" % "3.9.2",
  "com.lihaoyi"          %% "requests"     % "0.8.2",
  "org.apache.poi"        % "poi-ooxml"    % "5.2.0",
  "com.vladsch.flexmark"  % "flexmark-all" % "0.64.8"
)

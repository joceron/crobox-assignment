name := "crobox-assignment"

version := "0.1"

scalaVersion := "2.13.6"

val http4sVersion = "0.23.5"
val circeVersion  = "0.14.1"

libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect"         % "3.2.9"
  , "org.http4s"    %% "http4s-blaze-server" % http4sVersion
  , "org.http4s"    %% "http4s-dsl"          % http4sVersion
  , "org.http4s"    %% "http4s-circe"        % http4sVersion
  , "org.slf4j"      % "slf4j-simple"        % "1.7.32"
  , "io.circe"      %% "circe-core"          % circeVersion
  , "io.circe"      %% "circe-parser"        % circeVersion
  , "org.scalatest" %% "scalatest"           % "3.2.10" % Test
)

scalacOptions += "-deprecation"

scalaVersion := "2.12.6"

val http4sV = "0.18.23"

libraryDependencies ++= Seq(
   "org.http4s" %% "http4s-core"         % http4sV,
   "org.http4s" %% "http4s-circe"        % http4sV,
   "org.http4s" %% "http4s-blaze-client" % http4sV,
   "org.specs2" %% "specs2-core"         % "4.5.1" % Test,
   "org.specs2" %% "specs2-scalacheck"   % "4.5.1" % Test,
   "io.circe"   %% "circe-testing"       % "0.9.3" % Test
)

scalaVersion := "2.12.6"

val http4sV = "0.18.23"

libraryDependencies ++= Seq(
   "org.http4s" %% "http4s-core"         % http4sV,
   "org.http4s" %% "http4s-circe"        % http4sV,
   "org.http4s" %% "http4s-blaze-client" % http4sV
)

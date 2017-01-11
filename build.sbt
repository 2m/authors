lazy val authors = project
  .in(file("."))
  .aggregate(core)

lazy val core = project
  .settings(
    name := "authors-core",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"       % "10.0.1",
      "io.circe"          %% "circe-generic"   % "0.7.0-M2",
      "io.circe"          %% "circe-streaming" % "0.7.0-M2",
      "io.iteratee"       %% "iteratee-files"  % "0.8.0",
      "org.scalatest"     %% "scalatest"       % "3.0.1"     % Test,
      "com.typesafe.akka" %% "akka-testkit"    % "2.4.16"    % Test
    )
  )

inThisBuild(
  organization := "lt.dvim"
)

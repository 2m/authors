lazy val authors = project
  .in(file("."))
  .aggregate(core)

lazy val core = project
  .settings(
    name := "authors-core",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-streams"    % "2.5.10",
      "com.typesafe.akka" %% "akka-http"       % "10.0.1",
      "io.circe"          %% "circe-generic"   % "0.7.0-M1",
      "io.circe"          %% "circe-streaming" % "0.7.0-M1",
      "org.scalatest"     %% "scalatest"       % "3.0.1"     % Test,
      "com.typesafe.akka" %% "akka-testkit"    % "2.4.16"    % Test
    )
  )

inThisBuild(
  organization := "lt.dvim"
)

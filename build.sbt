lazy val authors = project
  .in(file("."))
  .aggregate(core)

lazy val core = project
  .settings(
    name := "authors-core",
    resolvers += Resolver.bintrayRepo("jypma", "maven"),
    resolvers += Resolver.bintrayRepo("readytalk", "maven"), // to resolve deps of ts-reaktive
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"               % "10.0.5",
      "com.tradeshift"    % "ts-reaktive-marshal-akka" % "0.0.23",
      "org.scalatest"     %% "scalatest"               % "3.0.1" % Test,
      "com.typesafe.akka" %% "akka-testkit"            % "2.4.17" % Test
    )
  )

inThisBuild(
  organization := "lt.dvim"
)

lazy val authors = project
  .in(file("."))
  .aggregate(core)

lazy val core = project
  .settings(
    name := "authors-core",
    resolvers += Resolver.bintrayRepo("jypma", "maven"),
    resolvers += Resolver.bintrayRepo("readytalk", "maven"), // to resolve deps of ts-reaktive
    {
      val Akka = "2.5.6"
      val AkkaHttp = "10.0.10"
      libraryDependencies ++= Seq(
        "com.typesafe.akka"    %% "akka-stream"              % Akka,
        "com.typesafe.akka"    %% "akka-http"                % AkkaHttp,
        "com.tradeshift"       %% "ts-reaktive-marshal-akka" % "0.0.30",
        "com.madgag.scala-git" %% "scala-git"                % "4.0",
        "org.scalatest"        %% "scalatest"                % "3.0.1" % Test,
        "com.typesafe.akka"    %% "akka-testkit"             % Akka % Test
      )
    }
  )

inThisBuild(
  Seq(
    organization := "lt.dvim.authors",
    scalafmtOnCompile := true
  )
)

lazy val authors = project
  .in(file("."))
  .aggregate(core, plugin)

lazy val core = project
  .settings(
    name := "authors-core",
    resolvers += Resolver.bintrayRepo("jypma", "maven"), {
      val Akka = "2.5.9"
      val AkkaHttp = "10.0.11"
      libraryDependencies ++= Seq(
        "com.typesafe.akka"    %% "akka-actor"               % Akka,
        "com.typesafe.akka"    %% "akka-stream"              % Akka,
        "com.typesafe.akka"    %% "akka-http"                % AkkaHttp,
        "com.tradeshift"       %% "ts-reaktive-marshal-akka" % "0.0.33",
        "com.madgag.scala-git" %% "scala-git"                % "4.0",
        "org.scalatest"        %% "scalatest"                % "3.0.4" % Test,
        "com.typesafe.akka"    %% "akka-testkit"             % Akka % Test
      )
    }
  )

lazy val plugin = project
  .dependsOn(core)
  .settings(
    name := "sbt-authors",
    sbtPlugin := true,
    scriptedLaunchOpts += ("-Dproject.version=" + version.value),
    scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
      a => Seq("-Xmx", "-Xms", "-XX", "-Dfile").exists(a.startsWith)
    ),
    scriptedDependencies := {
      val p1 = (publishLocal in core).value
      val p2 = publishLocal.value
    },
    scriptedBufferLog := false
  )

inThisBuild(
  Seq(
    organization := "lt.dvim.authors",
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("https://github.com/2m/authors")),
    scmInfo := Some(ScmInfo(url("https://github.com/2m/authors"), "git@github.com:2m/authors.git")),
    developers += Developer("contributors",
                            "Contributors",
                            "https://gitter.im/2m/authors",
                            url("https://github.com/2m/authors/graphs/contributors")),
    bintrayOrganization := Some("2m"),
    scalafmtOnCompile := true
  )
)

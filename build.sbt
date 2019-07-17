lazy val authors = project
  .in(file("."))
  .aggregate(core, plugin)

lazy val core = project
  .settings(
    name := "authors-core",
    // stick to 2.12.4 until 2.12.9 comes out with the following fixed:
    // https://github.com/scala/bug/issues/11373
    scalaVersion := "2.12.4",
    resolvers += Resolver.bintrayRepo("jypma", "maven"), {
      val Akka = "2.5.23"
      val AkkaHttp = "10.1.9"
      libraryDependencies ++= Seq(
        "com.typesafe.akka"    %% "akka-actor"               % Akka,
        "com.typesafe.akka"    %% "akka-stream"              % Akka,
        "com.typesafe.akka"    %% "akka-slf4j"               % Akka,
        "com.typesafe.akka"    %% "akka-http"                % AkkaHttp,
        "com.tradeshift"       %% "ts-reaktive-marshal-akka" % "0.14.0" exclude ("org.slf4j", "slf4j-log4j12"),
        "com.madgag.scala-git" %% "scala-git"                % "4.2",
        "ch.qos.logback"       % "logback-classic"           % "1.2.3",
        "org.scalatest"        %% "scalatest"                % "3.0.8" % "test",
        "com.typesafe.akka"    %% "akka-testkit"             % Akka % "test"
      )
    }
  )
  .enablePlugins(AutomateHeaderPlugin)

lazy val plugin = project
  .dependsOn(core)
  .enablePlugins(SbtPlugin, AutomateHeaderPlugin)
  .settings(
    name := "sbt-authors",
    scriptedLaunchOpts += ("-Dproject.version=" + version.value),
    scriptedDependencies := {
      val p1 = (publishLocal in core).value
      val p2 = publishLocal.value
    },
    scriptedBufferLog := false
  )

inThisBuild(
  Seq(
    organization := "lt.dvim.authors",
    organizationName := "Martynas Mickevičius",
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    startYear := Some(2016),
    homepage := Some(url("https://github.com/2m/authors")),
    scmInfo := Some(ScmInfo(url("https://github.com/2m/authors"), "git@github.com:2m/authors.git")),
    developers += Developer(
        "contributors",
        "Contributors",
        "https://gitter.im/2m/authors",
        url("https://github.com/2m/authors/graphs/contributors")
      ),
    bintrayOrganization := Some("2m"),
    scalafmtOnCompile := true,
    // show full stack traces and test case durations
    testOptions in Test += Tests.Argument("-oDF")
  )
)

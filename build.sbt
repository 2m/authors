val ScalaVersion = "2.12.15"

lazy val authors = project
  .in(file("."))
  .settings(sonatypeProfileName := "lt.dvim")
  .aggregate(core, plugin, cli)

lazy val core = project
  .settings(
    name := "authors-core",
    scalaVersion := ScalaVersion, {
      val Akka = "2.6.18"
      val AkkaHttp = "10.2.7"
      val Circe = "0.14.1"
      libraryDependencies ++= Seq(
        "com.typesafe.akka"    %% "akka-actor"                         % Akka,
        "com.typesafe.akka"    %% "akka-stream"                        % Akka,
        "com.typesafe.akka"    %% "akka-slf4j"                         % Akka,
        "com.typesafe.akka"    %% "akka-http"                          % AkkaHttp,
        "com.madgag.scala-git" %% "scala-git"                          % "4.2",
        "ch.qos.logback"        % "logback-classic"                    % "1.2.10",
        "org.mdedetrich"       %% "akka-stream-circe"                  % "0.8.0",
        "com.lightbend.akka"   %% "akka-stream-alpakka-json-streaming" % "3.0.3",
        "io.circe"             %% "circe-generic"                      % Circe,
        "io.circe"             %% "circe-generic-extras"               % Circe,
        "org.scalatest"        %% "scalatest"                          % "3.2.11" % "test",
        "com.typesafe.akka"    %% "akka-testkit"                       % Akka     % "test"
      )
    },
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
    )
  )
  .enablePlugins(AutomateHeaderPlugin)

lazy val plugin = project
  .dependsOn(core)
  .enablePlugins(SbtPlugin, AutomateHeaderPlugin)
  .settings(
    name := "sbt-authors",
    scriptedLaunchOpts += ("-Dproject.version=" + version.value),
    scriptedDependencies := {
      val p1 = (core / publishLocal).value
      val p2 = publishLocal.value
    },
    scriptedBufferLog := false
  )

lazy val cli = project
  .dependsOn(core)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    name := "authors-cli",
    scalaVersion := ScalaVersion,
    libraryDependencies ++= Seq(
      "org.rogach" %% "scallop" % "4.1.0"
    )
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
    scalafmtOnCompile := true,
    scalafixDependencies ++= Seq(
      "com.nequissimus" %% "sort-imports" % "0.6.1"
    ),
    // show full stack traces and test case durations
    Test / testOptions += Tests.Argument("-oDF")
  )
)

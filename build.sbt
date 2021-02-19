val ScalaVersion = "2.12.10"

lazy val authors = project
  .in(file("."))
  .aggregate(core, plugin, cli)

lazy val core = project
  .settings(
    name := "authors-core",
    scalaVersion := ScalaVersion,
    resolvers += Resolver.bintrayRepo("jypma", "maven"), {
      val Akka = "2.6.12"
      val AkkaHttp = "10.2.3"
      libraryDependencies ++= Seq(
        "com.typesafe.akka"    %% "akka-actor"               % Akka,
        "com.typesafe.akka"    %% "akka-stream"              % Akka,
        "com.typesafe.akka"    %% "akka-slf4j"               % Akka,
        "com.typesafe.akka"    %% "akka-http"                % AkkaHttp,
        "com.tradeshift"       %% "ts-reaktive-marshal-akka" % "0.16.3" exclude ("org.slf4j", "slf4j-log4j12"),
        "com.madgag.scala-git" %% "scala-git"                % "4.2",
        "ch.qos.logback"        % "logback-classic"          % "1.2.3",
        "org.scalatest"        %% "scalatest"                % "3.2.5" % "test",
        "com.typesafe.akka"    %% "akka-testkit"             % Akka    % "test",
        // these come from ts-reaktive-marshal-akka
        "com.typesafe.akka" %% "akka-persistence"       % Akka,
        "com.typesafe.akka" %% "akka-remote"            % Akka,
        "com.typesafe.akka" %% "akka-cluster"           % Akka,
        "com.typesafe.akka" %% "akka-cluster-tools"     % Akka,
        "com.typesafe.akka" %% "akka-distributed-data"  % Akka,
        "com.typesafe.akka" %% "akka-persistence-query" % Akka,
        "com.typesafe.akka" %% "akka-cluster-sharding"  % Akka,
        "com.typesafe.akka" %% "akka-protobuf"          % Akka,
        "com.typesafe.akka" %% "akka-http-jackson"      % AkkaHttp
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

lazy val cli = project
  .dependsOn(core)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    name := "authors-cli",
    scalaVersion := ScalaVersion,
    libraryDependencies ++= Seq(
      "org.rogach" %% "scallop" % "4.0.2"
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
    bintrayOrganization := Some("2m"),
    scalafmtOnCompile := true,
    scalafixDependencies ++= Seq(
      "com.nequissimus" %% "sort-imports" % "0.5.5"
    ),
    // show full stack traces and test case durations
    testOptions in Test += Tests.Argument("-oDF")
  )
)

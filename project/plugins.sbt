addSbtPlugin("com.dwijnand"      % "sbt-dynver"            % "2.0.0")
addSbtPlugin("com.dwijnand"      % "sbt-travisci"          % "1.1.1")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt-coursier" % "1.14")
addSbtPlugin("org.foundweekends" % "sbt-bintray"           % "0.5.1+12-0f116e2a")

libraryDependencies += { "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value }

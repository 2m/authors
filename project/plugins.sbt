addSbtPlugin("com.dwijnand"      % "sbt-dynver"   % "3.0.0")
addSbtPlugin("com.dwijnand"      % "sbt-travisci" % "1.1.1")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt" % "1.4.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray"  % "0.5.3")

libraryDependencies += { "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value }

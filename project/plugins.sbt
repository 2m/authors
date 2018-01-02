addSbtPlugin("com.dwijnand"      % "sbt-dynver"   % "2.0.0")
addSbtPlugin("com.dwijnand"      % "sbt-travisci" % "1.1.1")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt" % "1.4.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray"  % "0.5.2")

libraryDependencies += { "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value }

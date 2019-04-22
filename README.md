# [sbt-authors][] [![scaladex-badge][]][scaladex] [![travis-badge][]][travis] [![gitter-badge][]][gitter]

[sbt-authors]:        https://github.com/2m/authors
[scaladex]:           https://index.scala-lang.org/2m/authors
[scaladex-badge]:     https://index.scala-lang.org/2m/authors/latest.svg
[travis]:             https://travis-ci.org/2m/authors
[travis-badge]:       https://travis-ci.org/2m/authors.svg?branch=master
[gitter]:             https://gitter.im/2m/authors
[gitter-badge]:       https://badges.gitter.im/2m/authors.svg
[asciicast]:          https://asciinema.org/a/221093
[asciicast-badge]:    https://asciinema.org/a/221093.svg

`authors` is a CLI application and an [sbt](https://www.scala-sbt.org) plugin that produces a nicely formatted summary of authors that contributed to a project between two points in git history.

Inspired by:
* The original idea by [@rkuhn](https://github.com/rkuhn) who came up with [authors.pl](https://github.com/akka/akka/blob/v2.5.6/scripts/authors.pl) using now unknown archaic language
* The rewrite of the original script by [@johanandren](https://github.com/johanandren) who gave us the magnificent [authors.scala](https://github.com/akka/akka/blob/v2.5.6/scripts/authors.scala)

[![asciicast-badge][]][asciicast]

## Setup

Add this to your sbt build plugins, in either `project/plugins.sbt` or `~/.sbt/1.0/plugins/build.sbt`:

```scala
addSbtPlugin("lt.dvim.authors" % "sbt-authors" % "1.0.2")
resolvers += Resolver.bintrayRepo("jypma", "maven") // for ts-reaktive
```

`sbt-authors` is an AutoPlugin and therefore that is all that is required.

## Tasks

* `authors <from> <to>` Fetches the authors summary between two tags and puts it to your clipboard.
* `authorsFile <from> <to>` Writes the same authors summary to a `target/authors.md` file.

## Example usage

* `sbt "authors v0.20 v0.21"`

  Fetches the authors summary between `v0.20` and `v0.21` tags and puts it to your clipboard
  
* `sbt "authors v0.20 HEAD"`

  Fetches the authors summary between `v0.20` tag and the last commit and puts it to your clipboard
  
* `coursier launch -r bintray:jypma/maven lt.dvim.authors::authors-core:1.0.2 -- akka/alpakka v0.19 v0.20 ./`

  This will use [`coursier`](https://github.com/coursier/coursier) to launch the tool directly. You will have to be in the checkedout project directory when running this command. It will print out the authors summary to stdout. 
  
* `coursier bootstrap -r bintray:jypma/maven lt.dvim.authors:authors-core_2.12:1.0.2 -o authors `

  This will create an executable `authors` which then can be used to launch the application with the same arguments as mentioned above: `authors akka/alpakka v0.19 v0.20 ./`


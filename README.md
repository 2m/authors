# [sbt-authors][] [![scaladex-badge][]][scaladex] [![ci-badge][]][ci] [![gitter-badge][]][gitter]

[sbt-authors]:        https://github.com/2m/authors
[scaladex]:           https://index.scala-lang.org/2m/authors
[scaladex-badge]:     https://index.scala-lang.org/2m/authors/latest.svg
[ci]:                 https://github.com/2m/authors/actions
[ci-badge]:           https://github.com/2m/authors/workflows/ci/badge.svg
[gitter]:             https://gitter.im/2m/authors
[gitter-badge]:       https://badges.gitter.im/2m/authors.svg
[asciicast]:          https://asciinema.org/a/221093
[asciicast-badge]:    https://asciinema.org/a/221093.svg

`authors` is a CLI application and an [sbt](https://www.scala-sbt.org) plugin that produces a nicely formatted summary of authors that contributed to a project between two points in git history.

Inspired by:
* The original idea by [@rkuhn](https://github.com/rkuhn) who came up with [authors.pl](https://github.com/akka/akka/blob/v2.5.6/scripts/authors.pl) using now unknown archaic language
* The rewrite of the original script by [@johanandren](https://github.com/johanandren) who gave us the magnificent [authors.scala](https://github.com/akka/akka/blob/v2.5.6/scripts/authors.scala)

[![asciicast-badge][]][asciicast]

## Usage of the sbt plugin

Add this to your sbt build plugins, in either `project/plugins.sbt` or `~/.sbt/1.0/plugins/build.sbt`:

```scala
addSbtPlugin("lt.dvim.authors" % "sbt-authors" % "1.3")
```

`sbt-authors` is an AutoPlugin and therefore that is all that is required.

### Tasks

* `authors <from> <to>` Fetches the authors summary between two points in git history and prints it to stdout. For example:

    `authors v0.20 v0.21` - summary between `v0.20` and `v0.21` git tags

    `authors v0.20 HEAD` - summary between `v0.20` tag and the last commit

* `authorsFile <from> <to>` Writes the same authors summary to a `target/authors.md` file.
* `authorsClipboard <from> <to>` Puts the same authors summary to your clipboard.

## Usage of the CLI tool

Use [`coursier`](https://github.com/coursier/coursier) to install and launch `authors` directly.

    cs install --contrib authors

Then go to the checkedout folder of your project repository and run the foillowing.

    authors v1.1 v1.2

This will fetch the summary between the two tags, which will be printed to the stdout.

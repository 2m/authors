# [sbt-authors][] [![scaladex-badge][]][scaladex] [![travis-badge][]][travis] [![gitter-badge][]][gitter]

[sbt-authors]:        https://github.com/2m/authors
[scaladex]:           https://index.scala-lang.org/2m/authors
[scaladex-badge]:     https://index.scala-lang.org/2m/authors/latest.svg
[travis]:             https://travis-ci.org/2m/authors
[travis-badge]:       https://travis-ci.org/2m/authors.svg?branch=master
[gitter]:             https://gitter.im/2m/authors
[gitter-badge]:       https://badges.gitter.im/2m/authors.svg

`sbt-authors` is an [sbt](https://www.scala-sbt.org) plugin that produces a nicely formatted summary of authors that contributed to a project between two points in git history.

Inspired by:
* The original idea by [@rkuhn](https://github.com/rkuhn) who came up with [authors.pl](https://github.com/akka/akka/blob/v2.5.6/scripts/authors.pl) using now unknown archaic language
* The rewrite of the original script by [@johanandren](https://github.com/johanandren) who gave us the magnificent [authors.scala](https://github.com/akka/akka/blob/v2.5.6/scripts/authors.scala)

## Setup

Add this to your sbt build plugins, in either `project/plugins.sbt` or `~/.sbt/1.0/plugins/build.sbt`:

```scala
addSbtPlugin("lt.dvim.authors" % "sbt-authors" % "<latest version>")
resolvers += Resolver.bintrayRepo("jypma", "maven") // for ts-reaktive
```

`sbt-authors` is an AutoPlugin and therefore that is all that is required.

## Tasks

* `authors <from> <to>` Fetches the authors summary between two tags and puts it to your clipboard.

## Licence

Copyright 2016-2017 Martynas Mickevičius

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

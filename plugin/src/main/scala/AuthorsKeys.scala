package lt.dvim.authors

import sbt._

trait AuthorsKeys {
  val authors = inputKey[Unit]("Generate authors report.")
}

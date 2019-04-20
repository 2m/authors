package lt.dvim.authors

import sbt._

trait AuthorsKeys {
  val authors = inputKey[Unit]("Generate authors report to clipboard.")
  val authorsFile = inputKey[File]("Generate authors report to a file.")
}

/*
 * Copyright 2016 Martynas Mickevičius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lt.dvim.authors

import sbt._
import sbt.Keys._
import sbt.complete.DefaultParsers._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object AuthorsPlugin extends AutoPlugin {
  object autoImport extends AuthorsKeys
  import autoImport._

  override def trigger = AllRequirements
  override def projectSettings: Seq[Setting[_]] = authorsProjectSettings

  private final val ArgsParser = spaceDelimited("<from> <to>")

  def authorsProjectSettings: Seq[Setting[_]] = Seq(
    authors := {
      import scala.concurrent.ExecutionContext.Implicits.global
      val summary = authorsSummary(
        ArgsParser.parsed,
        scmInfo.value.orElse((ThisBuild / scmInfo).value),
        baseDirectory.value,
        streams.value
      ).map { s =>
        println(s)
        streams.value.log.info("Authors summary written to stdout.")
      }

      Await.result(summary, 30.seconds)
    },
    authors / aggregate := false,
    authorsFile := {
      import scala.concurrent.ExecutionContext.Implicits.global
      val summary = authorsSummary(
        ArgsParser.parsed,
        scmInfo.value.orElse((ThisBuild / scmInfo).value),
        baseDirectory.value,
        streams.value
      ).map { s =>
        val file = baseDirectory.value / "target" / "authors.md"
        IO.write(file, s)
        streams.value.log.info(s"Authors summary written to ${file.getAbsoluteFile}")
        file
      }

      Await.result(summary, 30.seconds)
    },
    authorsFile / aggregate := false,
    authorsClipboard := {
      import scala.concurrent.ExecutionContext.Implicits.global
      val summary = authorsSummary(
        ArgsParser.parsed,
        scmInfo.value.orElse((ThisBuild / scmInfo).value),
        baseDirectory.value,
        streams.value
      ).map { s =>
        import java.awt.Toolkit
        import java.awt.datatransfer._
        val clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
        val selection = new StringSelection(s)
        clipboard.setContents(selection, selection)

        streams.value.log.info("Authors summary placed into the clipboard.")
      }

      Await.result(summary, 30.seconds)
    },
    authorsClipboard / aggregate := false
  )

  private def authorsSummary(
      args: Seq[String],
      scmInfo: Option[ScmInfo],
      baseDirectory: File,
      streams: TaskStreams
  ): Future[String] = {
    val (from, to) = args match {
      case Seq(from, to) => (from, to)
      case _ =>
        sys.error("Please specify the <from> and <to> tags as the arguments to the task.")
    }

    // get the org/repo name from the scm info
    val repo = scmInfo.map(_.browseUrl.getPath.drop(1)).getOrElse {
      sys.error("Please set the scmInfo setting.")
    }

    streams.log.info(s"Fetching authors summary for $repo between $from and $to")

    Authors.summary(repo, from, to, baseDirectory.getAbsolutePath)
  }
}

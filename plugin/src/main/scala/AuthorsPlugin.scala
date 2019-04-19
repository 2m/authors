package lt.dvim.authors

import sbt._
import sbt.Keys._
import complete.DefaultParsers._

import scala.concurrent.Await
import scala.concurrent.duration._

object AuthorsPlugin extends AutoPlugin {
  object autoImport extends AuthorsKeys
  import autoImport._

  override def trigger = AllRequirements
  override def projectSettings: Seq[Setting[_]] = authorsSettings(Compile)

  def authorsSettings(config: Configuration): Seq[Setting[_]] = authorsGlobalSettings ++ inConfig(config)(Seq())

  def authorsGlobalSettings: Seq[Setting[_]] = Seq(
    authors := {
      val (from, to) = spaceDelimited("<from> <to>").parsed match {
        case Seq(from, to) => (from, to)
        case _ =>
          sys.error("Please specify the <from> and <to> tags as the arguments to the task.")
      }

      // get the org/repo name from the scm info
      val repo = scmInfo.value.map(_.browseUrl.getPath.drop(1)).getOrElse {
        sys.error("Please set the scmInfo setting.")
      }

      streams.value.log.info(s"Fetching authors summary for $repo between $from and $to")

      import scala.concurrent.ExecutionContext.Implicits.global
      val summary = Authors
        .summary(repo, from, to, baseDirectory.value.getAbsolutePath)
        .map { s =>
          import java.awt.Toolkit
          import java.awt.datatransfer._
          val clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
          val selection = new StringSelection(s)
          clipboard.setContents(selection, selection)

          streams.value.log.info("Authors summary placed into the clipboard.")
        }

      Await.result(summary, 30.seconds)
    },
    aggregate in authors := false
  )
}

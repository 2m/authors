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

import java.io.File

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.marshalling.PredefinedToRequestMarshallers._
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import com.tradeshift.reaktive.marshal.stream.{ActsonReader, ProtocolReader}
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.util.io.DisabledOutputStream
import com.madgag.git._
import lt.dvim.authors.GithubProtocol.{AuthorStats, Commit, Stats}
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import scala.collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

object Authors {
  final val MaxAuthors = 1024
  final val GithubApiUrl = "api.github.com"

  def main(args: Array[String]) = {
    val (repo, from, to, path) = args.toList match {
      case repo :: from :: to :: path :: Nil => (repo, from, to, path)
      case _ =>
        println("""
            |Usage:
            |  <repo> <from> <to> <path>
          """.stripMargin)
        System.exit(1)
        ???
    }

    val future = summary(repo, from, to, path)
    println(Await.result(future, 30.seconds))
  }

  def summary(repo: String, from: String, to: String, path: String): Future[String] = {
    val cld = classOf[ActorSystem].getClassLoader
    implicit val sys = ActorSystem("Authors", classLoader = Some(cld))
    implicit val gitRepository = Authors.gitRepo(path)
    implicit val log = Logging(sys, this.getClass)

    import sys.dispatcher

    DiffSource(repo, from, to)
      .via(ActsonReader.instance)
      .via(ProtocolReader.of(GithubProtocol.compareProto))
      .via(StatsAggregator())
      .via(SortingMachine())
      .via(MarkdownConverter())
      .runFold("")(_ ++ "\n" ++ _)
      .transformWith { res =>
        for {
          _ <- sys.terminate()
          r <- Future.fromTry(res)
        } yield r
      }
  }

  def gitRepo(path: String): FileRepository =
    FileRepositoryBuilder
      .create(new File(if (path.contains(".git")) path else path + "/.git"))
      .asInstanceOf[FileRepository]

  def shaToStats(sha: String)(implicit repo: FileRepository): Stats = {
    implicit val (revWalk, reader) = repo.singleThreadedReaderTuple
    val df = new DiffFormatter(DisabledOutputStream.INSTANCE)
    df.setRepository(repo)
    diff(abbrId(sha).asRevTree, abbrId(sha).asRevCommit.getParent(0).asRevTree)
      .flatMap { d =>
        df.toFileHeader(d).toEditList.asScala.map { edit =>
          Stats(
            additions = edit.getEndA - edit.getBeginA,
            deletions = edit.getEndB - edit.getBeginB,
            1
          )
        }
      }
      .fold(Stats(0, 0, 1))((sum, stats) => Stats(sum.additions + stats.additions, sum.deletions + stats.deletions, 1))
  }
}

object DiffSource {
  def apply(repo: String, from: String, to: String)(
      implicit ec: ExecutionContext,
      sys: ActorSystem
  ): Source[ByteString, NotUsed] =
    Source
      .future(
        Marshal(Uri(s"/repos/$repo/compare/$from...$to"))
          .to[HttpRequest]
      )
      .via(Http().outgoingConnectionHttps(Authors.GithubApiUrl))
      .map(_.entity.dataBytes)
      .flatMapConcat(identity)
}

object SortingMachine {
  def apply(): Flow[AuthorStats, AuthorStats, NotUsed] =
    Flow[AuthorStats]
      .grouped(Authors.MaxAuthors)
      .mapConcat(_.sortBy(s => (s.stats.commits, s.stats.additions, s.stats.deletions)).reverse)
}

object StatsAggregator {
  def apply()(implicit repo: FileRepository, log: LoggingAdapter): Flow[Commit, AuthorStats, NotUsed] =
    Flow[Commit]
      .filterNot(_.message.startsWith("Merge pull request"))
      .groupBy(Authors.MaxAuthors, commit => commit.githubAuthor.map(_.login).getOrElse(commit.gitAuthor.email))
      .log("Commit")
      .map(commit => AuthorStats(commit.gitAuthor, commit.githubAuthor, Authors.shaToStats(commit.sha)))
      .reduce((aggr, elem) =>
        aggr.copy(
          stats = Stats(
            additions = aggr.stats.additions + elem.stats.additions,
            deletions = aggr.stats.deletions + elem.stats.deletions,
            aggr.stats.commits + elem.stats.commits
          )
        )
      )
      .mergeSubstreams
}

object MarkdownConverter {
  def apply(): Flow[AuthorStats, String, NotUsed] =
    Flow[AuthorStats]
      .map { author =>
        val authorId = author.githubAuthor.map { gh =>
          // using html instead of markdown, because default
          // avatars come from github not resized
          s"""[<img width="20" alt="${gh.login}" src="${gh.avatar}&amp;s=40"/> **${gh.login}**](${gh.url})"""
        } getOrElse {
          author.gitAuthor.name
        }

        s"| $authorId | ${author.stats.commits} | ${author.stats.additions} | ${author.stats.deletions} |"
      }
      .prepend(
        Source(
          List(
            "| Author | Commits | Lines added | Lines removed |",
            "| ------ | ------- | ----------- | ------------- |"
          )
        )
      )
}

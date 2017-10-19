package lt.dvim.authors

import java.io.File

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.marshalling.PredefinedToRequestMarshallers._
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import com.tradeshift.reaktive.marshal.stream.{ActsonReader, ProtocolReader}
import com.typesafe.config.ConfigFactory
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.util.io.DisabledOutputStream
import com.madgag.git._
import lt.dvim.authors.GithubProtocol.{AuthorStats, Commit, Stats}
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import scala.collection.JavaConversions._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

object Authors {
  final val MaxAuthors = 1024

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
    val config = ConfigFactory.parseString("""
        |akka.loglevel = ERROR
      """.stripMargin)

    val cld = classOf[ActorSystem].getClassLoader
    implicit val sys =
      ActorSystem("Authors", config.withFallback(ConfigFactory.load(cld)), cld)
    implicit val mat = ActorMaterializer()
    implicit val gitRepository = Authors.gitRepo(path)

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
          _ <- Http().shutdownAllConnectionPools()
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
        df.toFileHeader(d).toEditList.toList.map { edit =>
          Stats(
            additions = edit.getEndA - edit.getBeginA,
            deletions = edit.getEndB - edit.getBeginB,
            1
          )
        }
      }
      .reduce((sum, stats) => Stats(sum.additions + stats.additions, sum.deletions + stats.deletions, 1))
  }
}

object DiffSource {
  def apply(repo: String, from: String, to: String)(implicit ec: ExecutionContext,
                                                    sys: ActorSystem,
                                                    mat: Materializer): Source[ByteString, NotUsed] =
    Source
      .fromFuture(
        Marshal(Uri(s"https://api.github.com/repos/$repo/compare/$from...$to"))
          .to[HttpRequest]
          .flatMap(Http().singleRequest(_))
          .map(_.entity.dataBytes)
      )
      .flatMapConcat(identity _)
}

object SortingMachine {
  def apply(): Flow[AuthorStats, AuthorStats, NotUsed] =
    Flow[AuthorStats]
      .grouped(Authors.MaxAuthors)
      .mapConcat(_.sortBy(s => (s.stats.commits, s.stats.additions, s.stats.deletions)).reverse)
}

object StatsAggregator {
  def apply()(implicit repo: FileRepository): Flow[Commit, AuthorStats, NotUsed] =
    Flow[Commit]
      .filterNot(_.message.startsWith("Merge pull request"))
      .groupBy(Authors.MaxAuthors, commit => commit.githubAuthor.map(_.login).getOrElse(commit.gitAuthor.email))
      .map(commit => AuthorStats(commit.gitAuthor, commit.githubAuthor, Authors.shaToStats(commit.sha)))
      .reduce(
        (aggr, elem) =>
          aggr.copy(
            stats = Stats(additions = aggr.stats.additions + elem.stats.additions,
                          deletions = aggr.stats.deletions + elem.stats.deletions,
                          aggr.stats.commits + elem.stats.commits)
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
          s"""[<img width="20" alt="${gh.login}" src="${gh.avatar}&s=40"> **${gh.login}**](${gh.url})"""
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

package lt.dvim.authors

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.marshalling.PredefinedToRequestMarshallers._
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.tradeshift.reaktive.marshal.stream.{ActsonReader, ProtocolReader}
import com.typesafe.config.ConfigFactory
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.util.io.DisabledOutputStream
import com.madgag.git._
import scala.collection.JavaConversions._

object Authors extends App {

  val (repo, from, to) = args.toList match {
    case repo :: from :: to :: Nil => (repo, from, to)
    case _ =>
      println("""
        |Usage:
        |  <repo> <from> <to>
      """.stripMargin)
      System.exit(1)
      ???
  }

  val config = ConfigFactory.parseString("""
      |akka.loglevel = DEBUG
    """.stripMargin)

  implicit val sys = ActorSystem("Authors", config.withFallback(ConfigFactory.load))
  implicit val mat = ActorMaterializer()

  import sys.dispatcher

  val completion = diffSource(repo, from, to)
    .via(ActsonReader.instance)
    .via(ProtocolReader.of(GithubProtocol.compareProto))
    .runForeach(println)

  scala.io.StdIn.readLine()

  sys.terminate()

  def diffSource(repo: String, from: String, to: String): Source[ByteString, NotUsed] =
    Source
      .fromFuture(
        Marshal(Uri(s"https://api.github.com/repos/$repo/compare/$from...$to"))
          .to[HttpRequest]
          .flatMap(Http().singleRequest(_))
          .map(_.entity.dataBytes)
      )
      .flatMapConcat(identity)

  def shaToStats(sha: String)(implicit repo: FileRepository): (Int, Int) = {
    implicit val (revWalk, reader) = repo.singleThreadedReaderTuple
    val df = new DiffFormatter(DisabledOutputStream.INSTANCE)
    df.setRepository(repo)
    diff(abbrId(sha).asRevTree, abbrId(sha).asRevCommit.getParent(0).asRevTree).flatMap { d =>
      df.toFileHeader(d).toEditList.toList.map { edit =>
        (edit.getEndA - edit.getBeginA, edit.getEndB - edit.getBeginB)
      }
    }.fold((0, 0))((sum, stats) => (sum._1 + stats._1, sum._2 + stats._2))
  }

}

package lt.dvim.authors

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.marshalling.PredefinedToRequestMarshallers._
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.tradeshift.reaktive.marshal.stream.{ActsonReader, ProtocolReader}
import com.typesafe.config.ConfigFactory

object Authors extends App {

  val (repo, from, to) = args.toList match {
    case repo :: from :: to :: Nil => (repo, from, to)
    case _ =>
      println("""
        |Usage:
        |  <repo> <from> <to>
      """.stripMargin)
      System.exit(1)
  }

  val config = ConfigFactory.parseString("""
      |akka.loglevel = DEBUG
    """.stripMargin)

  implicit val sys = ActorSystem("Authors", config.withFallback(ConfigFactory.load))
  implicit val mat = ActorMaterializer()

  import sys.dispatcher

  val completion = response(s"https://api.github.com/repos/$repo/compare/$from...$to")
    .via(ActsonReader.instance)
    .via(ProtocolReader.of(GithubProtocol.compareProto))
    .flatMapConcat { commit =>
      response(commit.url)
        .via(ActsonReader.instance)
        .via(ProtocolReader.of(GithubProtocol.commitProto))
    }
    .runForeach(println)

  scala.io.StdIn.readLine()

  sys.terminate()

  def response(url: String) =
    Source
      .fromFuture(
        Marshal(Uri(url))
          .to[HttpRequest]
          .flatMap(Http().singleRequest(_))
          .map(_.entity.dataBytes)
      )
      .flatMapConcat(identity)

}

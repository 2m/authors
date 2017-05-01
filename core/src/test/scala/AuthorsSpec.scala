package lt.dvim.authors

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink}
import akka.testkit.TestKit
import com.tradeshift.reaktive.marshal.stream.{ActsonReader, ProtocolReader}
import com.typesafe.config.ConfigFactory
import lt.dvim.authors.GithubProtocol.{Commit, CommitUrl}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.concurrent.duration._

object AuthorsSpec {
  val config = ConfigFactory.parseString("""
      |akka.loglevel = DEBUG
    """.stripMargin)
}

class AuthorsSpec
    extends TestKit(ActorSystem("AuthorsSpec", AuthorsSpec.config.withFallback(ConfigFactory.load)))
    with WordSpecLike
    with BeforeAndAfterAll
    with Matchers
    with Inside
    with ScalaFutures {

  implicit val mat             = ActorMaterializer()
  implicit val defaultPatience = PatienceConfig(timeout = 5.seconds, interval = 30.millis)

  "authors" should {
    "parse compare json" in {
      val res = FileIO
        .fromPath(Paths.get(getClass.getResource("/compare.json").toURI), 64)
        .via(ActsonReader.instance)
        .via(ProtocolReader.of(GithubProtocol.compareProto))
        .runFold(Seq.empty[CommitUrl])(_ :+ _)

      res.futureValue should have length 43
    }

    "parse commit json" in {
      val res = FileIO
        .fromPath(Paths.get(getClass.getResource("/commit.json").toURI), 64)
        .via(ActsonReader.instance)
        .via(ProtocolReader.of(GithubProtocol.commitProto))
        .runWith(Sink.head)

      inside(res.futureValue) {
        case Commit(Some(author), _, _) => author.login should be("2m")
      }
    }

    "parse commit with no github author json" in {
      val res = FileIO
        .fromPath(Paths.get(getClass.getResource("/commit_no_github_author.json").toURI), 64)
        .via(ActsonReader.instance)
        .via(ProtocolReader.of(GithubProtocol.commitProto))
        .runWith(Sink.head)

      inside(res.futureValue) {
        case Commit(None, author, _) => author.name should be("Veiga Ortiz, Héctor")
      }
    }
  }

  override def afterAll() =
    TestKit.shutdownActorSystem(system)

}

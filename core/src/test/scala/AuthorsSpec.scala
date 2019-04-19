package lt.dvim.authors

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.testkit.TestKit
import com.tradeshift.reaktive.marshal.stream.{ActsonReader, ProtocolReader}
import com.typesafe.config.ConfigFactory
import lt.dvim.authors.GithubProtocol._
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

  implicit val mat = ActorMaterializer()
  implicit val log = Logging(system, this.getClass)
  implicit val defaultPatience = PatienceConfig(timeout = 5.seconds, interval = 30.millis)

  "authors" should {
    "parse compare json" in {
      val res = FileIO
        .fromPath(Paths.get(getClass.getResource("/compare.json").toURI), 64)
        .via(ActsonReader.instance)
        .via(ProtocolReader.of(GithubProtocol.compareProto))
        .runFold(Seq.empty[Commit])(_ :+ _)

      res.futureValue should have length 2
    }

    "get commit stats from sha" in {
      implicit val repo = Authors.gitRepo(".git/modules/core/src/test/resources/authors-test-repo")
      Authors.shaToStats("e5fee6f") shouldBe Stats(additions = 4, deletions = 1, commits = 1)
    }

    "get stats when multiple chunks invloved" in {
      implicit val repo = Authors.gitRepo(".git/modules/core/src/test/resources/authors-test-repo")
      val stats = Source(
        List(
          Commit(
            "f576a45",
            "message",
            GitAuthor("test", "test@test.lt"),
            Some(GithubAuthor("test", "http://users/test", "http://avatars/test"))
          ),
          Commit(
            "bce0e63",
            "message",
            GitAuthor("test", "test@test.lt"),
            Some(GithubAuthor("test", "http://users/test", "http://avatars/test"))
          )
        )
      ).via(StatsAggregator())
        .runWith(Sink.head)

      whenReady(stats) {
        _ should matchPattern {
          case AuthorStats(_, _, Stats(9, 0, 2)) =>
        }
      }
    }

    "get stats when different emails but same github login" in {
      implicit val repo = Authors.gitRepo(".git/modules/core/src/test/resources/authors-test-repo")
      val stats = Source(
        List(
          Commit(
            "f576a45",
            "message",
            GitAuthor("test", "test1@test.lt"),
            Some(GithubAuthor("test", "http://users/test", "http://avatars/test"))
          ),
          Commit(
            "bce0e63",
            "message",
            GitAuthor("test", "test2@test.lt"),
            Some(GithubAuthor("test", "http://users/test", "http://avatars/test"))
          )
        )
      ).via(StatsAggregator())
        .runWith(Sink.head)

      whenReady(stats) {
        _ should matchPattern {
          case AuthorStats(_, _, Stats(9, 0, 2)) =>
        }
      }
    }

    "get stats when added file is binary" in {
      implicit val repo = Authors.gitRepo(".git/modules/core/src/test/resources/authors-test-repo")
      val stats = Source(
        List(
          Commit(
            "901392a",
            "message",
            GitAuthor("test", "test1@test.lt"),
            Some(GithubAuthor("test", "http://users/test", "http://avatars/test"))
          )
        )
      ).via(StatsAggregator())
        .runWith(Sink.head)

      whenReady(stats) {
        _ should matchPattern {
          case AuthorStats(_, _, Stats(0, 0, 1)) =>
        }
      }
    }
  }

  override def afterAll() =
    TestKit.shutdownActorSystem(system)

}

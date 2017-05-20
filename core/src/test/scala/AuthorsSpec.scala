package lt.dvim.authors

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink}
import akka.testkit.TestKit
import com.tradeshift.reaktive.marshal.stream.{ActsonReader, ProtocolReader}
import com.typesafe.config.ConfigFactory
import lt.dvim.authors.GithubProtocol.{Commit, Stats}
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
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
        .runFold(Seq.empty[Commit])(_ :+ _)

      res.futureValue should have length 2
    }

    "get commit stats from sha" in {
      implicit val repo = Authors.gitRepo(".git/modules/core/src/test/resources/authors-test-repo")
      Authors.shaToStats("e5fee6f") shouldBe Stats(additions = 4, deletions = 1)
    }
  }

  override def afterAll() =
    TestKit.shutdownActorSystem(system)

}

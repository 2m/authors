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

import java.nio.file.Paths

import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.testkit.TestKit

import com.typesafe.config.ConfigFactory
import org.mdedetrich.akka.stream.support.CirceStreamSupport
import org.scalatest.{BeforeAndAfterAll, Inside}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import lt.dvim.authors.Authors._
import lt.dvim.authors.GithubProtocol._

object AuthorsSpec {
  val config = ConfigFactory.parseString("""
      |akka.loglevel = DEBUG
    """.stripMargin)
}

class AuthorsSpec
    extends TestKit(ActorSystem("AuthorsSpec", AuthorsSpec.config.withFallback(ConfigFactory.load)))
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with Matchers
    with Inside
    with ScalaFutures {
  implicit val log = Logging(system, this.getClass)
  implicit val defaultPatience = PatienceConfig(timeout = 5.seconds, interval = 30.millis)

  "authors" should {
    "parse compare json" in {
      val res = FileIO
        .fromPath(Paths.get(getClass.getResource("/compare.json").toURI), 64)
        .via(JsonReader.select("$.commits[*]"))
        .via(CirceStreamSupport.decode[Commit])
        .runFold(Seq.empty[Commit])(_ :+ _)

      res.futureValue should contain theSameElementsInOrderAs Seq(
        Commit(
          "03ac2f41efff9cdfac9419ba7e6e34b30f9111e0",
          GitCommit("Add some contents", GitAuthor("Martynas Mickevičius", "martynas@2m.lt")),
          Some(GithubAuthor("2m", "https://github.com/2m", "https://avatars0.githubusercontent.com/u/422086?v=3"))
        ),
        Commit(
          "e5fee6fbc982cea605a820c82a8ae8f14ead26e0",
          GitCommit("Add some other contents", GitAuthor("Test User", "test.user@2m.lt")),
          None
        )
      )
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
            GitCommit("message", GitAuthor("test", "test@test.lt")),
            Some(GithubAuthor("test", "http://users/test", "http://avatars/test"))
          ),
          Commit(
            "bce0e63",
            GitCommit("message", GitAuthor("test", "test@test.lt")),
            Some(GithubAuthor("test", "http://users/test", "http://avatars/test"))
          )
        )
      ).via(StatsAggregator())
        .runWith(Sink.head)

      whenReady(stats) {
        _ should matchPattern { case AuthorStats(_, _, Stats(9, 0, 2)) =>
        }
      }
    }

    "get stats when different emails but same github login" in {
      implicit val repo = Authors.gitRepo(".git/modules/core/src/test/resources/authors-test-repo")
      val stats = Source(
        List(
          Commit(
            "f576a45",
            GitCommit("message", GitAuthor("test", "test1@test.lt")),
            Some(GithubAuthor("test", "http://users/test", "http://avatars/test"))
          ),
          Commit(
            "bce0e63",
            GitCommit("message", GitAuthor("test", "test2@test.lt")),
            Some(GithubAuthor("test", "http://users/test", "http://avatars/test"))
          )
        )
      ).via(StatsAggregator())
        .runWith(Sink.head)

      whenReady(stats) {
        _ should matchPattern { case AuthorStats(_, _, Stats(9, 0, 2)) =>
        }
      }
    }

    "get stats when added file is binary" in {
      implicit val repo = Authors.gitRepo(".git/modules/core/src/test/resources/authors-test-repo")
      val stats = Source(
        List(
          Commit(
            "901392a",
            GitCommit("message", GitAuthor("test", "test1@test.lt")),
            Some(GithubAuthor("test", "http://users/test", "http://avatars/test"))
          )
        )
      ).via(StatsAggregator())
        .runWith(Sink.head)

      whenReady(stats) {
        _ should matchPattern { case AuthorStats(_, _, Stats(0, 0, 1)) =>
        }
      }
    }
  }

  override def afterAll() =
    TestKit.shutdownActorSystem(system)
}

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

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.testkit.TestKit
import com.tradeshift.reaktive.marshal.stream.{ActsonReader, ProtocolReader}
import com.typesafe.config.ConfigFactory
import lt.dvim.authors.GithubProtocol._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Inside}

import scala.concurrent.duration._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

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

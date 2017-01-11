package lt.dvim.authors

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import akka.testkit.TestKit
import cats.implicits._
import io.circe.streaming._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class AuthorsSpec extends TestKit(ActorSystem("AuthorsSpec")) with WordSpecLike with BeforeAndAfterAll with ScalaFutures {

  import lt.dvim.AkkaStreamIterateeIoSpec._
  //import system.dispatcher
  //import scala.concurrent.ExecutionContext.Implicits.global



  implicit val mat = ActorMaterializer()
  implicit val defaultPatience = PatienceConfig(timeout = 5.seconds, interval = 30.millis)

  "authors" should {
    "parse" in {
      import system.dispatcher

      val futModule = io.iteratee.modules.future
      val futFileModule = io.iteratee.files.future

      val parse = byteParser[Future]
      val decode = decoder[Future, Commit]

      val source = FileIO.fromPath(Paths.get("/home/martynas/projects/authors/core/target/scala-2.11/test-classes/diff.json"), 64)

      val res = source
        .map(_.toArray)
        .via(new EnumerateeIoStage(parse))
        .runForeach(println)

      res.futureValue
    }

    "parse with circe" in {
      import scala.concurrent.ExecutionContext.Implicits.global

      val futModule = io.iteratee.modules.future(scala.concurrent.ExecutionContext.Implicits.global)
      val futFileModule = io.iteratee.files.future(scala.concurrent.ExecutionContext.Implicits.global)

      import futModule._
      import futFileModule._

      val commits = listFiles(new File("/home/martynas/projects/authors/core/target/scala-2.11/test-classes/diff_array.json"))//.through(byteParser).through(decoder[Future, Commit])

      //val commits = iterate(1)(_ + 1)

      val total = commits.into(takeI(1))

      //total.failed.futureValue.printStackTrace()

      println(total.futureValue)

      Thread.sleep(5000)
    }
  }

  override def afterAll() =
    TestKit.shutdownActorSystem(system)

}

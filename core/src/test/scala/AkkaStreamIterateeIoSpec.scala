package lt.dvim

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.testkit.TestKit
import cats.Eval
import cats.data.NonEmptyList
import cats.implicits._
import io.iteratee._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

class AkkaStreamIterateeIoSpec extends TestKit(ActorSystem("AkkaStreamIterateeIoSpec")) with WordSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll {
  import AkkaStreamIterateeIoSpec._
  import system.dispatcher

  implicit val mat = ActorMaterializer()
  implicit val defaultPatience = PatienceConfig(timeout = 5.seconds, interval = 30.millis)

  "akka stream" should {
    "run iteratee" ignore {
      import io.iteratee.modules.eval._

      val naturals = iterate(1)(_ + 1)
      val mult3or5 = filter[Int](i => i % 3 == 0 || i % 5 == 0)
      val multsUnder1000 = takeWhile[Int](_ < 1000).compose(mult3or5)
      val sum = consume[Int].map(_.sum)

      println(naturals.through(multsUnder1000).into(sum).value)

      def printer(l: NonEmptyList[Int]): Iteratee[Eval, Int, Int] = {
        println(s"got ${l.toList.size} elements")
        cont[Int, Int](printer, Eval.now(1))
      }

      val iter = cont[Int, Int](printer, Eval.now(1))
      naturals.through(multsUnder1000).into(iter).value
    }

    "run on futures" ignore {
      implicit val futModule = io.iteratee.modules.future
      import futModule._

      val futureEnum = iterateM(0) {
        case old => Future.successful(old + 1)
      }

      val mult3or5 = filter[Int](i => i % 3 == 0 || i % 5 == 0)
      val multsUnder1000 = takeWhile[Int](_ < 10)

      val oneByOne = foldM(0) {
        (count, element: Int) =>
          println(s"got $element")
          Future.successful(count + 1)
      }

      val res = futureEnum.through(multsUnder1000).through(mult3or5).into(oneByOne)

      Await.result(res, 1.second)
    }

    "complete on futures" in {
      implicit val futModule = io.iteratee.modules.future
      import futModule._

      val promise = Promise[Int]
      val futureEnum = iterateM(0) {
        case old => promise.future
      }
      //promise.success(1)

      val takeOne = take[Int](0)

      val oneByOne = foldM(0) {
        (count, element: Int) =>
          println(s"got $element")
          Future.successful(count + 1)
      }

      val res = futureEnum.through(takeOne).into(oneByOne)

      res.futureValue shouldBe 0
    }

    "complete an empty stream" in {
      implicit val futModule = io.iteratee.modules.future
      import futModule._

      val mapEnumeratee = map[Int, Int](_ * 2)
      val mapped = Source.empty[Int].via(new EnumerateeIoStage(mapEnumeratee)).runWith(Sink.headOption)

      mapped.futureValue shouldBe None
    }

    "run one-to-one transformation of one element" in {
      implicit val futModule = io.iteratee.modules.future
      import futModule._

      val mapEnumeratee = map[Int, Int](_ * 2)
      val mapped = Source.single(1).via(new EnumerateeIoStage(mapEnumeratee)).runWith(Sink.head)

      mapped.futureValue shouldBe 2
    }

    "run one-to-many transformation of one element" in {
      implicit val futModule = io.iteratee.modules.future
      import futModule._

      val enumeratee = flatMap[Int, Int](el => enumList(List(el, el, el)))
      val result = Source.single(1).via(new EnumerateeIoStage(enumeratee)).grouped(3).runWith(Sink.head)

      result.futureValue shouldBe Seq(1, 1, 1)
    }

    "complete when inner stage is finished" in {
      implicit val futModule = io.iteratee.modules.future
      import futModule._

      val enumeratee = take[Int](3)
      val result = Source.repeat(1).via(new EnumerateeIoStage(enumeratee)).runWith(Sink.fold(0)(_ + _))

      result.futureValue shouldBe 3
    }

  }

  override def afterAll() =
    TestKit.shutdownActorSystem(system)

}

object AkkaStreamIterateeIoSpec {

  final class EnumerateeIoStage[In, Out](e: Enumeratee[Future, In, Out])(implicit val ec: ExecutionContext) extends GraphStage[FlowShape[In, Out]] {

    implicit val futModule = io.iteratee.modules.future(ec)
    import futModule._

    val in = Inlet[In]("EnumerateeIo.in")
    val out = Outlet[Out]("EnumerateeOut.out")

    override val shape = FlowShape.of(in, out)
    override protected def initialAttributes: Attributes = Attributes.name("EnumerateeIoStage")

    override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

      def runInnerPipeline(first: In) = {
        println("running inner pipeline")
        iterateM(first) { _ =>
          println("setting up future for enumeratee element")
          val promise = Promise[In]
          onInPromise.invoke(promise)
          promise.future
        }.through(e).into(foldM(()) { (_, outElement) =>
          println(s"got element from enumeratee $outElement")
          onOutElement.invoke(outElement)
          Future.successful()
        })
      }

      var inPromise: Option[Promise[In]] = None
      var innerPipeline: Option[Future[Unit]] = None

      private var inBuffer = Option.empty[In]
      private var outBuffer = Queue.empty[Out]

      private val onInPromise = getAsyncCallback[Promise[In]] { promise =>
        println(s"setting inPromise ${innerPipeline.get.isCompleted}")

        inBuffer.fold { inPromise = Some(promise); } { inElement =>
          promise.success(inElement)
          println(s"completing promise ${innerPipeline.get.isCompleted}")
          inBuffer = None
//          if (!hasBeenPulled(in)) {
//            pull(in)
//          }
        }
      }

      private val onOutElement = getAsyncCallback[Out] { outElement =>
        println(s"onOutElement async callback ${isAvailable(out)} ${outBuffer}")
        outBuffer = outBuffer :+ outElement
        if (isAvailable(out)) {
          push(out, outBuffer.head)
          outBuffer = outBuffer.tail
        }
//        if (outBuffer.isEmpty) {
//          innerPipeline.fold() { pipeline =>
//            if (pipeline.isCompleted) {
//              println("completing stage")
//              completeStage()
//            }
//          }
//        }
      }

      val initialInHandler = new InHandler {
        override def onPush() = {
          val el = grab(in)
          println(s"onPush in the initial $el")
          innerPipeline = Some(runInnerPipeline(el))
          setHandler(in, inHandler)
        }

        override def onUpstreamFinish() = {
          innerPipeline.fold(completeStage()) { inner =>
            if (inner.isCompleted)
              completeStage()
          }
          println("upstream has finished in the initial handler")
        }

        override def onUpstreamFailure(ex: Throwable) = {
          println(s"upstream has failed ${ex.getMessage}")
          ex.printStackTrace()
        }
      }

      val inHandler = new InHandler {
        override def onPush() = {
          val el = grab(in)
          println(s"onPush in the regular $el")
          println(s"inPromise: $inPromise")
          println(s"inBuffer: $inBuffer")

          inPromise.fold {
            inBuffer = Some(el)
            println("buffering")
          } { promise =>
            promise.success(el)
            println("completing in promise")
          }
        }

        override def onUpstreamFinish() = {
          innerPipeline.fold(completeStage()) { inner =>
            if (inner.isCompleted)
              completeStage()
          }
          println("upstream has finished")
        }

        override def onUpstreamFailure(ex: Throwable) = {
          println(s"upstream has failed ${ex.getMessage}")
          ex.printStackTrace()
        }
      }

      setHandler(in, initialInHandler)
      setHandler(out,
        new OutHandler {
          override def onPull() = {
            println("on pull")

            if (outBuffer.isEmpty) {
              if (!isClosed(in)) pull(in)
//              innerPipeline.fold() { pipeline =>
//                if (pipeline.isCompleted)
//                  completeStage()
//              }
            } else {
              push(out, outBuffer.head)
              outBuffer = outBuffer.tail
            }
          }
        })
    }
  }

}

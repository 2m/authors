package lt.dvim

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.testkit.TestKit
import cats.{Eval, Monad}
import cats.data.NonEmptyList
import cats.instances.FutureInstances
import io.iteratee._
import io.iteratee.internal.Step
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

class AkkaStreamIterateeIoSpec extends TestKit(ActorSystem("AkkaStreamIterateeIoSpec")) with WordSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll {
  import AkkaStreamIterateeIoSpec._

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
      import AkkaStreamIterateeIoSpec.future._

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

    "complete an empty stream" in {
      import future._

      val mapEnumeratee = map[Int, Int](_ * 2)
      val mapped = Source.empty[Int].via(new EnumerateeIoStage(mapEnumeratee)).runWith(Sink.headOption)

      mapped.futureValue shouldBe None
    }

    "run one-to-one transformation of one element" in {
      import future._

      val mapEnumeratee = map[Int, Int](_ * 2)
      val mapped = Source.single(1).via(new EnumerateeIoStage(mapEnumeratee)).runWith(Sink.head)

      mapped.futureValue shouldBe 2
    }

    "run one-to-many transformation of one element" in {
      import future._

      val enumeratee = flatMap[Int, Int](el => enumList(List(el, el, el)))
      val result = Source.single(1).via(new EnumerateeIoStage(enumeratee)).grouped(3).runWith(Sink.head)

      result.futureValue shouldBe Seq(1, 1, 1)
    }

    "complete when inner stage is finished" in {
      import future._

      val enumeratee = take[Int](3)
      val result = Source.repeat(1).via(new EnumerateeIoStage(enumeratee)).runWith(Sink.fold(0)(_ + _))

      result.futureValue shouldBe 3
    }

  }

  override def afterAll() =
    TestKit.shutdownActorSystem(system)

}

object AkkaStreamIterateeIoSpec {

  final object future extends AkkaStreamIterateeIoSpec.FutureModule {
    implicit def ec = scala.concurrent.ExecutionContext.Implicits.global
  }

  trait FutureModule extends Module[Future]
    with EnumeratorModule[Future] with EnumerateeModule[Future] with IterateeModule[Future] with FutureInstances {
    final type M[f[_]] = Monad[f]
    final protected val F: Monad[Future] = catsStdInstancesForFuture
    implicit def ec: ExecutionContext
  }

  final class EnumerateeIoStage[In, Out](e: Enumeratee[Future, In, Out])(implicit val ec: ExecutionContext) extends GraphStage[FlowShape[In, Out]] with FutureModule {

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
          val promise = Promise[Unit]
          onOutPromise.invoke(promise, outElement)
          promise.future
        })
      }

      var inPromise: Option[Promise[In]] = None
      var outPromise: Option[Promise[Unit]] = None
      var bufferedOut: Option[Out] = None
      var innerPipeline: Option[Future[Unit]] = None

      private val onInPromise = getAsyncCallback[Promise[In]] { promise =>
        println("setting inPromise")
        inPromise = Some(promise)
        if (!hasBeenPulled(in)) {
          pull(in)
        }
      }

      private val onOutPromise = getAsyncCallback[(Promise[Unit], Out)] {
        case (promise, outElement) =>
          println(s"on out promise async callback ${isAvailable(out)}")
          if (isAvailable(out)) {
            push(out, outElement)
            promise.success()
          } else {
            outPromise = Some(promise)
            bufferedOut = Some(outElement)
          }
      }

      val initialInHandler = new InHandler {
        override def onPush() = {
          println("onPush in the initial")
          val el = grab(in)
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
      }

      val inHandler = new InHandler {
        override def onPush() = {
          println("onPush in the regular")
          val el = grab(in)
          inPromise.get.success(el)
        }

        override def onUpstreamFinish() = {
          innerPipeline.fold(completeStage()) { inner =>
            if (inner.isCompleted)
              completeStage()
          }
          println("upstream has finished")
        }

        override def onUpstreamFailure(ex: Throwable) = {
          println("upstream has failed")
          ex.printStackTrace()
        }
      }

      setHandler(in, initialInHandler)
      setHandler(out,
        new OutHandler {
          override def onPull() = {
            println("on pull")
            bufferedOut.fold(if (!isClosed(in) && (!innerPipeline.isDefined || inPromise.isDefined)) pull(in)) { el =>
              push(out, el)
              bufferedOut = None
              outPromise.get.success()
            }
          }
        })
    }
  }

}

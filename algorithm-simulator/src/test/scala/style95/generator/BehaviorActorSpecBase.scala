package style95.generator

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Assertion, BeforeAndAfterAll, Matchers, WordSpecLike}
import style95.generator.AverageTpsObserver.AverageTps

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.abs

@RunWith(classOf[JUnitRunner])
abstract class BehaviorActorSpecBase(name: String)
    extends TestKit(ActorSystem(name))
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  override def afterAll(): Unit = shutdown(system)
}

trait AverageTpsMatcher { this: BehaviorActorSpecBase =>
  def expectAverageTps(sender: BehaviorActorBuilder,
                       expectedTps: Int,
                       expectedSecs: Int,
                       epsilon: Double): Assertion = {
    val probe = TestProbe()
    system.actorOf(AverageTpsObserver.props(sender, probe.ref))
    val AverageTps(obTps, obSecs) =
      probe.expectMsgType[AverageTps](expectedSecs + 1 seconds)

    println(s"obTps: $obTps, expectedTps: $expectedTps")
    println(s"obSecs: $obSecs, expectedSecs: $expectedSecs")
    relativeError(obTps, expectedTps) should be <= epsilon
    relativeError(obSecs, expectedSecs) should be <= epsilon
  }

  private def relativeError(t: Double, e: Double) =
    if (e != 0) {
      abs(t - e) / e
    } else if (t == 0) {
      0d
    } else {
      throw new IllegalArgumentException("relative error is undefined")
    }
}

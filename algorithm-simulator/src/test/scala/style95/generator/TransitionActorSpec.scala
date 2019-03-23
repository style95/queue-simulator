package style95.generator

import scala.concurrent.duration._

class TransitionActorSpec
    extends BehaviorActorSpecBase("TransitionActorSpec")
    with AverageTpsMatcher {

  override def afterAll(): Unit = shutdown(system)

  "A transition TPS actor" should {
    "give the expected mean TPS in a smooth transition" in {
      testTransition(10, 30, 15)
    }

    "give the expected mean TPS in a sharp transition" in {
      testTransition(10, 1000, 5)
    }
  }

  private def testTransition(startTps: Int, endTps: Int, secs: Int) = {
    val behavior = Transition(startTps, endTps) in secs.seconds
    expectAverageTps(behavior, (startTps + endTps) / 2, secs, 0.1)
  }
}

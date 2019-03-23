package style95.generator

import scala.concurrent.duration._

class MaintainActorSpec
    extends BehaviorActorSpecBase("MaintainActorSpec")
    with AverageTpsMatcher {

  "A maintain TPS actor" should {
    "give similar mean TPS in a low TPS setting (TPS: 5, seconds: 15)" in {
      testMaintainActor(5, 20, 0.2)
    }
    "give similar mean TPS in a medium TPS setting (TPS: 100, seconds: 15)" in {
      testMaintainActor(100, 15, 0.1)
    }
    "give similar mean TPS in a high TPS setting (TPS: 10000, seconds: 15)" in {
      testMaintainActor(10000, 15, 0.1)
    }
  }

  private def testMaintainActor(expectedTps: Int,
                                expectedSecs: Int,
                                epsilon: Double) = {
    val behavior = Maintain(expectedTps) in expectedSecs.seconds
    expectAverageTps(behavior, expectedTps, expectedSecs, epsilon)
  }
}

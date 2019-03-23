package style95.generator

import scala.concurrent.duration._

class SequentialBehaviorActorSpec
    extends BehaviorActorSpecBase("SequentialBehaviorActorSpec")
    with AverageTpsMatcher {

  "A sequential behavior actor that consists of one behavior" should {
    "look identical to Maintain actor" in {
      val expectedTps = 100
      val expectedSecs = 15
      val sequence = ScenarioBuilder startsWith {
        Maintain(expectedTps) in expectedSecs.seconds
      }

      expectAverageTps(sequence, expectedTps, expectedSecs, 0.1)
    }

    "look identical to Transition actor" in {
      val (start, end, secs) = (20, 100, 15)
      val sequence = ScenarioBuilder startsWith {
        Transition(start, end) in secs.seconds
      }

      expectAverageTps(sequence, (start + end) / 2, secs, 0.1)
    }

    "A sequence behavior that consists of multiple behaviors" should {
      "be verifiable with mean TPS test" in {
        val (low, high, secsPerStage) = (0, 300, 10)
        val sequence = ScenarioBuilder startsWith {
          Transition(low, high) in secsPerStage.seconds
        } next {
          Maintain(high) in secsPerStage.seconds
        } next {
          Transition(high, low) in secsPerStage.seconds
        }

        // area of the trapeziod / total secs
        val expectedTps = (4 * secsPerStage * high) / 2 / (3 * secsPerStage)
        expectAverageTps(sequence, expectedTps, secsPerStage * 3, 0.1)
      }
    }
  }
}

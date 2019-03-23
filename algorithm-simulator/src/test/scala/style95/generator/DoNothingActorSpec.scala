package style95.generator

import scala.concurrent.duration._

class DoNothingActorSpec
    extends BehaviorActorSpecBase("DoNothingActorSpec")
    with AverageTpsMatcher {
  "A DoNothing TPS actor" should {
    "produce no observable TPS" in {
      val secs = 10
      val behavior = DoNothing in secs.seconds
      expectAverageTps(behavior, 0, secs, 0.01)
    }
  }
}

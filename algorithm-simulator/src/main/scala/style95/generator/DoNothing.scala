package style95.generator

import scala.concurrent.duration._
import scala.language.postfixOps

// Do not generate any request in a given period.
// It works as Maintain(0 tps).
case object DoNothing extends BehaviorDescriptor with Stationary {
  class Gen extends TimingGenerator {
    def next(elapsed: FiniteDuration): FiniteDuration = {
      // a little hacky
      10 days
    }
  }
  def timing: TimingGenerator = new Gen
}

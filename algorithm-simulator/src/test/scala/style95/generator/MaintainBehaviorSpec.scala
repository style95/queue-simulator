package style95.generator

import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

class MaintainBehaviorSpec extends WordSpecLike with Matchers {
  val epsilon = 0.05

  "A maintain TPS behavior" should {
    "generate correct request timing with low (1) TPS" in {
      testMaintainBehavior(1)
    }
    "generate correct request timing with medium (100) TPS" in {
      testMaintainBehavior(100)
    }
  }

  private def testMaintainBehavior(expectedTps: Int) = {
    val expectedInterval = (1.0 / expectedTps) * 1e9
    val sampleNumber = 10000

    val gen = Maintain(expectedTps).timing
    val start = System.nanoTime()
    val meanInterval = (1 to sampleNumber)
      .map(_ => gen.next((System.nanoTime() - start) nanos).toNanos)
      .sum / sampleNumber

    (meanInterval - expectedInterval) / expectedInterval should be <= epsilon
  }
}

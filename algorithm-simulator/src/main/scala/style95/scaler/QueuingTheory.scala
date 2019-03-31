package style95.scaler

import akka.actor.ActorRef
import style95.StatusLogger.PredictedTps

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.math.{ceil, max}

// Time series analysis + queuing theory + ad hoc rules
// Algorithm outline:
//   1. Use single exponential smoothing to forecast feature load.
//   2. Scaling out with queuing theory (M/M/1 queue) to meet schedule latency requirements.
//   3. Scaling in with ad hoc rules
class QueuingTheory(
    creatingHint: FiniteDuration, // a hint value for how long it takes to create a new container
    consultInterval: FiniteDuration, // the time interval between successive invocations of the scaler
    logger: ActorRef, // log status to file
    alpha: Double, // parameter of time series analysis
    slo: FiniteDuration, // schedule latency service level objective
    idleTimeout: FiniteDuration, // wKZait how long before killing an idle container
) extends Scaler {
  require(slo > 0.second, "latency requirement must be greater than zero")

  private var s = 0D
  // A container buffer is maintained to make sure an idle container won't be killed
  // prematurely before the idleTimeout is expired.
  private var idleBuffer = Queue.empty[(ActorRef, Long)]

  override def decide(info: DecisionInfo): Decision = {
    val DecisionInfo(elapse, in, _, invokeTime, existing, creating, queued) =
      info

    // Because container creation takes some time, we need a proactive policy to scaling out.
    // The number of look forward steps equals the container creation time divided by
    // the scaler invocation interval.
    val nSteps = forwardSteps(creatingHint, consultInterval)
    val forecast =
      forecastTps(in.toDouble / (consultInterval.toMillis / 1e3), nSteps)

    logger ! PredictedTps(elapse + nSteps * consultInterval.toNanos, forecast)
    // Since we are looking at the future, we can assume all the initialing containers to be ready.
    val readyContainer = existing.size + creating.size
    // a lower bound of container number is calculated the queuing equations.
    val lowerBound = deriveRequired(forecast, invokeTime)

    println(s"forecast: $forecast ,container: $readyContainer, lb: $lowerBound")

    val newAlloc = wakeUpIdle(max(lowerBound - readyContainer, 0))
    if (newAlloc > 0) {
      // scaling out
      AddContainer(newAlloc)
    } else if (queued == 0) {
      // scaling in
      // We only scale in when the queue is empty
      markAsIdle(max(readyContainer - lowerBound, 0), existing)
      evictTimeout() match {
        case Nil     => NoOp
        case victims => RemoveContainer(victims)
      }
    } else {
      NoOp
    }
  }

  private def forwardSteps(creatingHint: FiniteDuration,
                           consultInterval: FiniteDuration): Int =
    ceil(creatingHint.toMillis.toDouble / consultInterval.toMillis).toInt

  private def forecastTps(currentTps: Double, steps: Int): Double = {
    // single exponential smoothing
    s += alpha * (currentTps - s)
    s
  }

  private def deriveRequired(tps: Double, invokeTime: FiniteDuration): Int = {
    if (tps <= 0.01) {
      // The smoothing tps will only approaching zero.
      // Here a rather arbitrary threshold is applied to make sure
      // the container number can be scaled in to zero.
      0
    } else {
      val serveRate = 1e9D / invokeTime.toNanos
      val sloSecs = slo.toNanos / 1e9D
      // the number of server required to meet the required average latency.
      // This equation is not accurate. It's derived based on a simplified queuing model.
      ceil((1 + tps * sloSecs) / (serveRate * sloSecs)).toInt
    }
  }

  private def wakeUpIdle(number: Int): Int = {
    val spill = max(number - idleBuffer.size, 0)
    // prefer to wake up the newly marked containers
    idleBuffer = idleBuffer.dropRight(number)
    spill
  }

  private def markAsIdle(number: Int, candidates: List[ActorRef]): Unit = {
    // avoid repeated buffering
    val notMarked = candidates.filter(a =>
      !idleBuffer.exists {
        case (buffered, _) => a == buffered
    })
    idleBuffer = idleBuffer ++ notMarked
      .take(number)
      .map(a => (a, System.nanoTime()))
  }

  private def evictTimeout(): List[ActorRef] = {
    val num = idleBuffer.count {
      case (_, t) => (System.nanoTime() - t).nanos >= idleTimeout
    }
    val (evicted, remaining) = idleBuffer.splitAt(num)
    idleBuffer = remaining
    evicted.map(_._1).toList
  }
}

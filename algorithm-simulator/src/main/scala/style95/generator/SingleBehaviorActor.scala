package style95.generator

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Timers}
import style95.Container.ActivationMessage

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.language.postfixOps

object SingleBehaviorActor {
  def props(behavior: IntervalBehavior, receiver: ActorRef): Props =
    Props(new SingleBehaviorActor(behavior, receiver))

  final case object SendRequest
  private case object TickKey
  private case object GenerateHits
}

class SingleBehaviorActor(behavior: IntervalBehavior, receiver: ActorRef)
    extends Actor
    with ActorLogging
    with Timers {
  import SingleBehaviorActor._
  import context.{dispatcher, system}

  private val timingGen = behavior.behavior.timing
  private val start = System.nanoTime()
  private var preGeneratedHits = Queue.empty[Long]

  // Generate hits at frequently as possible. The more frequent ticks are generated,
  // the closer they conform to Poisson distribution
  private val schedulerPrecision =
    context.system.settings.config.getDuration("akka.scheduler.tick-duration")
  log.info(s"the timing accuracy of the actor scheduler is $schedulerPrecision")
  timers.startPeriodicTimer(TickKey, GenerateHits, schedulerPrecision)

  log.info(s"load generator will send requests to ${receiver.path.name}")
  system.scheduler.scheduleOnce(behavior.duration) {
    log.info(
      s"Interval ${behavior.duration} expired, shutting down actor ${self.path.name}")
    self ! PoisonPill
  }

  override def receive: Receive = {
    case SendRequest =>
      receiver ! ActivationMessage(self, System.nanoTime())
    case GenerateHits =>
      // Akka scheduler has limited timing precision. To compensate the coarse timing,
      // we may need to fire multiple hits in one tick so that the desired level of
      // tps can be reached.
      val now = System.nanoTime()
      // First, fire every hit that has a smaller timestamp then now
      while (preGeneratedHits.nonEmpty && preGeneratedHits.head <= now) {
        self ! SendRequest
        preGeneratedHits = preGeneratedHits.tail
      }
      // Then, pre-compute hits that should should be fired in next scheduler tick
      if (preGeneratedHits.isEmpty) {
        // Normally, there should always be at least one hit remaining in the queue.
        // However, a empty queue can show up at the first call of scheduleNext.
        val hit = now + timingGen.next((now - start) nanos).toNanos
        preGeneratedHits = preGeneratedHits.enqueue(hit)
        log.debug("empty queue encountered")
      }
      // Scheduler can be sluggish sometime, so we pre-generate for 2 scheduling intervals.
      val lookForward = now + 2 * schedulerPrecision.toNanos
      while (preGeneratedHits.last < lookForward) {
        val hit = preGeneratedHits.last + timingGen
          .next((preGeneratedHits.last - start) nanos)
          .toNanos
        preGeneratedHits = preGeneratedHits.enqueue(hit)
      }
  }
}

package style95.generator

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}

object SequentialBehaviorActor {
  def props(behaviors: List[IntervalBehavior], receiver: ActorRef) =
    Props(new SequentialBehaviorActor(behaviors, receiver))
}

class SequentialBehaviorActor(behaviors: List[IntervalBehavior],
                              receiver: ActorRef)
    extends Actor
    with ActorLogging {

  override def preStart(): Unit = {
    setupRemaining(behaviors)
  }

  override def receive: Receive = Actor.emptyBehavior

  private def waitForStageTermination(
      stage: ActorRef,
      remaining: List[IntervalBehavior]): Receive = {
    case Terminated(`stage`) =>
      log.info(
        s"interval behavior actor${stage.path.name} terminated, setting up next child actor")
      setupRemaining(remaining)
  }

  private def setupRemaining(remaining: List[IntervalBehavior]): Unit =
    remaining match {
      case head :: tail =>
        val nextStage =
          context.actorOf(SingleBehaviorActor.props(head, receiver))
        context.watch(nextStage)
        context.become(waitForStageTermination(nextStage, tail))
      case _ => {
        log.info("no remaining interval behavior, shutdown sequence actor")
        self ! PoisonPill
      }
    }
}

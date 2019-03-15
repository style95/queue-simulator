package style95

import akka.actor.{Actor, ActorRef, Props}
import style95.QueueSimulator.ConsultScaler

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.language.postfixOps

import Container._

object QueueSimulator {
  final case object ConsultScaler

  def props(scaler: Scaler,
            checkInterval: FiniteDuration,
            containerProps: ContainerProperty): Props =
    Props(new QueueSimulator(scaler, checkInterval, containerProps))
}

class QueueSimulator(scaler: Scaler,
                     checkInterval: FiniteDuration,
                     containerProps: ContainerProperty)
    extends Actor {

  import Scaler._
  import context.{system, dispatcher}

  private var queue = Queue.empty[ActivationMessage]
  private var existing = Map.empty[ActorRef, ContainerStatus]
  private var inProgress = Set.empty[ActorRef]

  private var inSinceLastTick = 0
  private var outSinceLastTick = 0

  private var scheduledNum = 0
  private var averageLatency = 0.0

  private var requester: Option[ActorRef] = None

  system.scheduler.schedule(0 seconds, checkInterval) {
    self ! ConsultScaler
  }

  override def receive: Receive = {
    case msg: ActivationMessage =>
      requester = Some(sender)

      inSinceLastTick += 1
      queue = queue.enqueue(msg)
      tryRunActions()
    case ContainerCreated =>
      inProgress -= sender
      existing += sender -> ContainerStatus(true)
      tryRunActions()
    case done: WorkDone =>
      outSinceLastTick += 1
      existing += sender -> ContainerStatus(true)
      requester.get ! done
      tryRunActions()
    case ConsultScaler =>
      println(
        s"in: $inSinceLastTick, current: ${queue.size}, out: $outSinceLastTick, existing: ${existing.size}, inProgress: ${inProgress.size}, averageLatency: $averageLatency")

      val decision = scaler.decide(
        DecideInfo(inSinceLastTick,
                   outSinceLastTick,
                   existing.size,
                   inProgress.size,
                   queue.size))
      decision match {
        case AddContainer(number) =>
          println(s"create $number containers")
          (1 to number) foreach { _ =>
            val container =
              context.actorOf(Container.props(self, containerProps))
            inProgress += container
          }
        case NoOp =>
      }

      inSinceLastTick = 0
      outSinceLastTick = 0
  }

  private def tryRunActions(): Unit = {
    val idles = idleContainers.iterator

    while (queue.nonEmpty && idles.hasNext) {
      val (msg, newQueue) = queue.dequeue
      val idle = idles.next()

      queue = newQueue
      existing += idle -> ContainerStatus(false)

      val latency = (System.nanoTime() - msg.startTime) / 1e6
      scheduledNum += 1
      averageLatency += 1.0 / scheduledNum * (latency - averageLatency)

      idle ! msg
    }
  }

  private def idleContainers =
    existing
      .filter {
        case (_, ContainerStatus(ready)) => ready
      }
      .map {
        case (actor, _) => actor
      }
}

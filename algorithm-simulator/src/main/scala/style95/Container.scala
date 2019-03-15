package style95

import akka.actor.{Actor, ActorRef, Props}
import style95.Container.ContainerProperty

import scala.concurrent.duration._

object Container {
  final case class ActivationMessage(startTime: Long)

  final case class WorkDone(startTime: Long)

  final case class ContainerStatus(ready: Boolean)

  final case object ContainerCreated

  final case class ContainerProperty(initialDelay: FiniteDuration,
                                     execTime: FiniteDuration)

  def props(simulator: ActorRef, property: ContainerProperty): Props =
    Props(new Container(simulator, property))
}

class Container(val simulator: ActorRef, property: ContainerProperty)
    extends Actor {

  import Container._
  import context.{system, dispatcher}

  system.scheduler.scheduleOnce(property.initialDelay) {
    simulator ! ContainerCreated
  }

  override def receive: Receive = {
    case ActivationMessage(start) =>
      system.scheduler.scheduleOnce(property.execTime) {
        simulator ! WorkDone(start)
      }
  }
}

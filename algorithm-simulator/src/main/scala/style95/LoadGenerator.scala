package style95

import akka.actor.{Actor, ActorRef, Props, Timers}
import scala.concurrent.duration._
import scala.language.postfixOps

object LoadGenerator {
  def props(simulator: ActorRef, tps: Int): Props =
    Props(new LoadGenerator(simulator, tps))

  private case object MsgTickerKey
  private case object MessageTick
}

class LoadGenerator(simulator: ActorRef, tps: Int) extends Actor with Timers {
  import Container._
  import LoadGenerator._

  timers.startPeriodicTimer(MsgTickerKey, MessageTick, (1e3 / tps) millis)

  override def receive: Receive = {
    case MessageTick =>
      simulator ! ActivationMessage(self, System.nanoTime())
    case WorkDone =>
  }
}

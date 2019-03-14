package style95

import akka.actor.{Actor, ActorRef, Props, Timers}
import scala.concurrent.duration._
import scala.language.postfixOps

object LoadGenerator {
  def props(simulator: ActorRef, tps: Int): Props =
    Props(new LoadGenerator(simulator, tps))

  private case object MsgTickerKey
  private case object ReportTickerKey
  private case object MessageTick
  private case object ReportTick
}

class LoadGenerator(simulator: ActorRef, tps: Int) extends Actor with Timers {
  import Container._
  import LoadGenerator._

  private var avgLatency = 0.0
  private var completedNumber = 0

  timers.startPeriodicTimer(MsgTickerKey, MessageTick, (1e3 / tps) millis)
  timers.startPeriodicTimer(ReportTickerKey, ReportTick, 100 millis)

  override def receive: Receive = {
    case MessageTick =>
      simulator ! ActivationMessage(System.nanoTime())
    case WorkDone(start) =>
      val latency = (System.nanoTime() - start) / 1e6
      completedNumber += 1
      // moving average
      avgLatency = avgLatency + 1.0 / completedNumber * (latency - avgLatency)
    case ReportTick =>
      println(s"average latency: $avgLatency")
  }
}

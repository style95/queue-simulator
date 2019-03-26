package style95.generator

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import style95.Container.ActivationMessage

object AverageTpsObserver {
  final case class AverageTps(tps: Double, secs: Double)

  def props(behavior: BehaviorActorBuilder, monitor: ActorRef): Props =
    Props(new AverageTpsObserver(behavior, monitor))
}

class AverageTpsObserver(sender: BehaviorActorBuilder, monitor: ActorRef)
    extends Actor
    with ActorLogging {
  import AverageTpsObserver._

  private val generator = context.actorOf(sender against self)
  context.watch(generator)
  private var hits = 0
  private val start = System.nanoTime()

  override def receive: Receive = {
    case _: ActivationMessage =>
      hits += 1
    case Terminated(`generator`) =>
      val secs = (System.nanoTime() - start).toDouble / 1e9
      val tps = hits / secs

      log.info(s"the watched generator has stopped, AverageTps($tps, $secs)")
      monitor ! AverageTps(tps, secs)
  }
}

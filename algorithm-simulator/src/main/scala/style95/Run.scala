package style95

import akka.actor.{ActorSystem}
import scala.concurrent.duration._
import scala.language.postfixOps

object Run extends App {
  println(s"[Simulator]")
  println(s"arguments:")
  println(
    s"  - [inMsg]: # of incoming messages per second | default: 50 messages per second")
  println(s"  - [exec]: execution time | default: 100 milliseconds")
  println(s"  - [create]: creation delay | default: 300 milliseconds")
  println(s"ex) ./gradlew runSimulator -PinMsg=50 -Pexec=100 -Pcreate=300")

  val incomingRate = if (args.length >= 1) args(0).toInt else 50
  val execTime = if (args.length >= 2) args(1).toInt else 1000
  val createTime = if (args.length >= 3) args(2).toInt else 300

  var containerProps =
    Container.ContainerProperty(createTime millis, execTime millis)
  val outgoing = 1000 / execTime // number of messages that one container handle in 1 second
  println(
    s"[incoming msg/s: $incomingRate | outgoing msg/s: $outgoing], execution time: $execTime, creation delay: ${createTime}")

  val actorSystem = ActorSystem("controller-actor-system")
  val scaler = new Scaler(1000) //TODO: the limit is not working yet
  val simulator =
    actorSystem.actorOf(QueueSimulator.props(scaler, 100 millis, containerProps), "simulator")
  val loadGenerator =
    actorSystem.actorOf(LoadGenerator.props(simulator, incomingRate), "generator")
}

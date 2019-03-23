package style95.scenarios

import akka.actor.ActorSystem
import akka.pattern.gracefulStop
import scala.concurrent.duration._
import scala.language.postfixOps
import style95._

abstract class ScenarioBase extends App {
  def actorBuilder: generator.BehaviorActorBuilder

  println("[Simulator]")
  println("arguments:")
  println("  - [scenario]: scenario class | default: Demo")
  println("  - [exec]: execution time | default: 100 milliseconds")
  println("  - [create]: container creation delay | default: 300 milliseconds")
  println("  - [outPath]: log output file path | default: out/status.csv")
  println(
    "ex) ./gradlew runScenario -Pscenario=Demo -Pexec=100 -Pcreate=300 -PoutPath=out/statusOne.csv")

  val execTime = if (args.length >= 1) args(0).toInt else 100
  val createTime = if (args.length >= 2) args(1).toInt else 300
  val outPath = if (args.length >= 3) args(2) else "out/status.csv"

  val containerProps =
    Container.ContainerProperty(createTime millis, execTime millis)
  val actorSystem = ActorSystem("QueueSimulatorSystem")
  val scaler = new Scaler(1000) // Limit is not working yet
  val logger = actorSystem.actorOf(StatusLogger.props(outPath), "status-logger")
  val simulator = actorSystem.actorOf(
    QueueSimulator.props(scaler, logger, 100 millis, containerProps))
  val logGenerator =
    actorSystem.actorOf(actorBuilder against simulator, "load-generator")

  Thread.sleep((actorBuilder.runningDuration + 1.seconds).toMillis)
  val stoppedFut = gracefulStop(logger, 5 seconds)
  actorSystem.terminate()
}

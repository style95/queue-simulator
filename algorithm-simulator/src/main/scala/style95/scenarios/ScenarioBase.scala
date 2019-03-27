package style95.scenarios

import akka.actor.ActorSystem
import akka.pattern.gracefulStop

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import style95._
import style95.scaler.Scaler

abstract class ScenarioBase extends App {
  def actorBuilder: generator.BehaviorActorBuilder
  def scaler: Scaler

  println("[Simulator]")
  println("arguments:")
  println("  - [scenario]: scenario class | required")
  println("  - [outDir]: log output file path | default: out/demo")
  println("  - [exec]: execution time | default: 100 milliseconds")
  println("  - [create]: container creation delay | default: 300 milliseconds")
  println(
    "ex) ./gradlew runScenario -Pscenario=Demo -PoutDir=out/demo -Pexec=100 -Pcreate=300 ")

  val outDir = if (args.length >= 1) args(0) else "out/demo"
  val execTime = if (args.length >= 2) args(1).toInt else 100
  val createTime = if (args.length >= 3) args(2).toInt else 300

  val containerProps =
    Container.ContainerProperty(createTime millis, execTime millis)
  val actorSystem = ActorSystem("QueueSimulatorSystem")
  val logger = actorSystem.actorOf(StatusLogger.props(outDir), "status-logger")
  val simulator = actorSystem.actorOf(
    QueueSimulator.props(scaler, logger, 100 millis, containerProps))
  val logGenerator =
    actorSystem.actorOf(actorBuilder against simulator, "load-generator")

  val duration = actorBuilder.runningDuration
  println(s"the scenario takes $duration")
  Thread.sleep((duration + 1.seconds).toMillis)
  val stoppedFut = gracefulStop(logger, 5 seconds)
  Await.result(stoppedFut, 5 seconds)

  actorSystem.terminate()
}

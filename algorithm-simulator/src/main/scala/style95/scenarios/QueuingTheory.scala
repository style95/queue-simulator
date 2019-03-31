package style95.scenarios

import akka.actor.ActorRef
import style95.Container.ContainerProperty
import style95.generator._
import style95.scaler._

import scala.concurrent.duration._
import scala.language.postfixOps

// A test with queuing theory. Spawn all container at once and make
// TPS fixed.
object QueuingTheory extends ScenarioBase {
  override def actorBuilder: BehaviorActorBuilder =
    ScenarioBuilder startsWith {
      DoNothing in 5.seconds
    } next {
      Transition(1, 100) in 5.seconds
    } next {
      Maintain(100) in 20.seconds
    } next {
      Transition(100, 1) in 5.seconds
    } next {
      DoNothing in 5.seconds
    } next {
      Maintain(50) in 5.seconds
    } next {
      Maintain(100) in 10.seconds
    } next {
      DoNothing in 5.seconds
    } next {
      Maintain(200) in 0.5.seconds
    } next {
      DoNothing in 5.seconds
    }

  override def buildScaler(creatingHint: FiniteDuration,
                           consultInterval: FiniteDuration,
                           logger: ActorRef) =
    new QueuingTheory(creatingHint,
                      consultInterval,
                      logger,
                      0.2,
                      20 millis,
                      5 seconds)

  override def containerProperty = ContainerProperty(300 millis, 100 millis)
}

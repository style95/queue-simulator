package style95.scenarios

import akka.actor.ActorRef
import style95.Container.ContainerProperty
import style95.generator._
import style95.scaler._

import scala.concurrent.duration._
import scala.language.postfixOps

object RampUpThenDown extends ScenarioBase {
  override def actorBuilder: BehaviorActorBuilder =
    ScenarioBuilder startsWith {
      DoNothing in 5.seconds
    } next {
      Transition(1, 100) in 5.seconds
    } next {
      Maintain(100) in 10.seconds
    } next {
      Transition(100, 1) in 5.seconds
    } next {
      DoNothing in 5.seconds
    }

  override def buildScaler(creatingHint: FiniteDuration,
                           consultInterval: FiniteDuration,
                           logger: ActorRef) = new DefaultScaler(1000)

  override def containerProperty = ContainerProperty(300 millis, 100 millis)

}

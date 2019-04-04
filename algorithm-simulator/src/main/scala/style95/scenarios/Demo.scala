package style95.scenarios

import akka.actor.ActorRef
import style95.Container.ContainerProperty
import style95.generator._
import style95.scaler._

import scala.concurrent.duration._
import scala.language.postfixOps

object Demo extends ScenarioBase {
  override def actorBuilder: BehaviorActorBuilder = Maintain(10) in 10.seconds

  override def buildScaler(creatingHint: FiniteDuration,
                           consultInterval: FiniteDuration,
                           logger: ActorRef) = new DefaultScaler(1000)

  override def containerProperty = ContainerProperty(300 millis, 100 millis)
}

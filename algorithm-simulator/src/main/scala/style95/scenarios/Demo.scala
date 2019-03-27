package style95.scenarios

import style95.generator._
import style95.scaler._

import scala.concurrent.duration._

object Demo extends ScenarioBase {
  override def actorBuilder: BehaviorActorBuilder = Maintain(10) in 10.seconds

  override def scaler: Scaler = new DefaultScaler(1000)
}

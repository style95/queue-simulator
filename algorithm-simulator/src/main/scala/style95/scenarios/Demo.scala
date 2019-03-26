package style95.scenarios
import style95.generator._
import scala.concurrent.duration._

object Demo extends ScenarioBase {
  val secs = 10
  val tps = 10
  override def actorBuilder: BehaviorActorBuilder = Maintain(tps) in secs.seconds
}

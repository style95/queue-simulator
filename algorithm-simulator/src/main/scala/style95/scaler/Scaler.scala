package style95.scaler

import scala.concurrent.duration.FiniteDuration
import akka.actor.ActorRef

final case class DecisionInfo(
    elapse: Long,
    in: Int, // number of new requests since last tick
    out: Int, // number of consumed requests since last tick
    invokeTime: FiniteDuration, // average invoking time in milliseconds
    existing: List[ActorRef], // created containers, busy or idle
    creating: List[ActorRef], // containers that are being initialized
    queued: Int // number of waiting requests
)

sealed trait Decision

final case class AddContainer(number: Int) extends Decision
// An implementation of the scaler should guarantee that it only evicts victims from
// the existing list; i.e., Containers undergoing initialization will not be removed.
final case class RemoveContainer(victims: List[ActorRef]) extends Decision
case object NoOp extends Decision

trait Scaler {
  def decide(info: DecisionInfo): Decision
}

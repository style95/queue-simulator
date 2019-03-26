package style95.scaler

final case class DecisionInfo(
    in: Int, // number of new requests since last tick
    out: Int, // number of consumed requests since last tick
    existing: Int, // created containers, busy or idle
    creating: Int, // containers that are being initialized
    queued: Int // number of waiting requests
)

sealed trait Decision

final case class AddContainer(number: Int) extends Decision
case object NoOp extends Decision

trait Scaler {
  def decide(info: DecisionInfo): Decision
}

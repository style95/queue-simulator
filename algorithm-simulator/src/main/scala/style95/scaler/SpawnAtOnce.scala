package style95.scaler

class SpawnAtOnce(number: Int) extends Scaler {
  override def decide(info: DecisionInfo): Decision = {
    if (info.existing == 0 && info.creating == 0) {
      AddContainer(number)
    } else {
      NoOp
    }
  }
}

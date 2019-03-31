package style95.scaler

class DefaultScaler(containerLimit: Int) extends Scaler {
  private var tick = 0
  private var totalIncome = 0
  private var totalConsumption = 0
  private var perConConsumptionPerTick = 0.0
  private var maxConsumptionInTick = -1.0

  override def decide(info: DecisionInfo): Decision = {
    var DecisionInfo(_, income, outcome, _, existing, inProgress, current) =
      info

    val (nExisting, nInProgress) = (existing.size, inProgress.size)

    totalIncome += income
    totalConsumption += outcome

    if (nExisting == 0 && nInProgress == 0) {
      return AddContainer(1)
    }

    if (totalConsumption > 0) {
      tick += 1
      perConConsumptionPerTick = totalConsumption.toDouble / nExisting / tick
    }

    maxConsumptionInTick = math.max(maxConsumptionInTick, outcome)
    val expectedTpt = perConConsumptionPerTick * (nExisting + nInProgress)

    println(
      s"perConConsumption: $perConConsumptionPerTick, expectedTpt: $expectedTpt, maxConsumptionInTick: $maxConsumptionInTick")

    val number =
      if (income >= expectedTpt && current > maxConsumptionInTick && expectedTpt != 0) {

        val criteria = (income / perConConsumptionPerTick) - nExisting - nInProgress
        val number = math.ceil(criteria).toInt
        println(
          s"criteria: $criteria = ($income / $perConConsumptionPerTick) - $nExisting - $nInProgress")
        number
      } else {
        0
      }

    if (number > 0) {
      AddContainer(number)
    } else {
      NoOp
    }
  }
}

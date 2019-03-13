package style95

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps

class QueueSimulator(val incoming: Int,
                     execTime: Int,
                     outgoing: Int,
                     createTime: Int)
    extends Actor {
  implicit val ec: ExecutionContextExecutor = context.dispatcher

  val incomingDuration: FiniteDuration = (1000 / incoming) milliseconds

  val createDuration = createTime milliseconds
  val execDuration = execTime milliseconds

  var queue: Queue[ActivationMessage] = Queue.empty[ActivationMessage]
  var currentContainers = 0
  var inprogressContainers = 0
  var sumOfLatency: Long = 0
  var numOfMsg: Long = 0

  var checkInterval: FiniteDuration = Duration(100L, "milliseconds")

  var in = 0
  var out = 0
  var totalConsumption = -1.0
  var perConConsumption = -1.0
  var maxTps = 0.0
  var tick = 0
  override def receive: Receive = {
    case msg: ActivationMessage =>
      in += 1
      queue = queue.enqueue(msg)

    case ActivationRequest =>
      if (queue.nonEmpty) {

        if (perConConsumption == -1.0) {
          perConConsumption = 0.0
          totalConsumption = 0.0
        }
        out += 1
        totalConsumption += 1
        val (msg, newQueue) = queue.dequeue
        queue = newQueue

        val latency = (System.nanoTime() - msg.startTime) / 1000000
        sumOfLatency += latency
        numOfMsg += 1

        sender ! msg
      }

    case ContainerCreated =>
      currentContainers += 1
      inprogressContainers -= 1

  }

  def ceiling(d: Double) = math.ceil(d).toInt

  def decide(income: Int,
             outcome: Int,
             existing: Int,
             inProgress: Int,
             current: Int,
             perConOutcome: Double,
             expectedTps: Double,
             maxExpectedTps: Double) = {
    if (existing == 0 && inProgress == 0) {
      inprogressContainers += 1
      context.actorOf(
        Props(new TestConsumer(self, createDuration, execDuration)))
    } else if (income >= expectedTps && current > maxExpectedTps && expectedTps != 0.0) {

      val criteria = (income.toDouble / perConOutcome) - existing - inProgress
      val num = ceiling(criteria)
      if (num > 0) {
        println(
          s"create ${num} containers: ${criteria} = (${income.toDouble} / ${perConOutcome}) - ${existing} - ${inProgress}")
        (1 to num).foreach { _ =>
          inprogressContainers += 1
          context.actorOf(
            Props(new TestConsumer(self, createDuration, execDuration)))
        }
      }
    }
  }

  context.system.scheduler.schedule(0 seconds, checkInterval) {
    if (totalConsumption != -1.0) {
      tick += 1
      perConConsumption = totalConsumption / tick / currentContainers
    }
    val expectedTps
      : Double = perConConsumption * (currentContainers + inprogressContainers)
    maxTps = if (maxTps > out) maxTps else out
    if (numOfMsg == 0) {
      println(
        s"in: ${in}, current: ${queue.length}, out: ${out}, existing: ${currentContainers}, inprogress: ${inprogressContainers}, perConConsumption: ${perConConsumption}, expectedTps: ${expectedTps}, maxExpectedTps: ${maxTps}, averageLatency: 0")
    } else {
      println(
        s"in: ${in}, current: ${queue.length}, out: ${out}, existing: ${currentContainers}, inprogress: ${inprogressContainers}, perConConsumption: ${perConConsumption}, expectedTps: ${expectedTps}, maxExpectedTps: ${maxTps}, averageLatency: ${sumOfLatency / numOfMsg} ms")

    }
    decide(in,
           out,
           currentContainers,
           inprogressContainers,
           queue.length,
           perConConsumption,
           expectedTps,
           maxTps)
    in = 0
    out = 0
  }

  context.system.scheduler.schedule(0 seconds, incomingDuration) {
    val startTime = System.nanoTime
    self ! ActivationMessage(startTime)
  }

}

object Simulator {
  def main(args: Array[String]): Unit = {
    println(s"[Simulator]")
    println(s"arguments:")
    println(s"  - [inMsg]: # of incoming messages per second | default: 50 messages per second")
    println(s"  - [exec]: execution time | default: 100 milliseconds")
    println(s"  - [create]: creation delay | default: 300 milliseconds")
    println(s"ex) ./gradlew runSimulator -PinMsg=50 -Pexec=100 -Pcreate=300")

    val incoming = if (args.length >= 1) args(0).toInt else 50
    val execTime = if (args.length >= 2) args(1).toInt else 1000
    val createTime = if (args.length >= 3) args(2).toInt else 300
    val outgoing = 1000 / execTime // number of messages that one container handle in 1 second

    println(
      s"[incoming msg/s: ${incoming} | outgoing msg/s: ${outgoing}], execution time: ${execTime}, creation delay: ${createTime}")

    implicit val actorSystem = ActorSystem("controller-actor-system")

    val simulator = actorSystem.actorOf(
      Props(new QueueSimulator(incoming, execTime, outgoing, createTime)))

    simulator ! ActivationMessage(System.nanoTime)
  }
}

class TestConsumer(val queue: ActorRef,
                   initialDelay: FiniteDuration,
                   interval: FiniteDuration)
    extends Actor {
  implicit val ec: ExecutionContextExecutor =
    context.system.dispatchers.lookup("dispatchers.consumer-dispatcher")

  override def receive: Receive = {
    case ActivationRequest =>
      queue ! ActivationRequest

    case _: ActivationMessage =>
      // do nothing
  }

  context.system.scheduler.scheduleOnce(initialDelay) {
    context.parent ! ContainerCreated
  }

  context.system.scheduler.schedule(initialDelay, interval) {
    self ! ActivationRequest
  }
}

case class ActivationMessage(startTime: Long)
case object ActivationRequest
case object ContainerCreated

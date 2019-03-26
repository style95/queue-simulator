package style95

import akka.actor.{Actor, ActorLogging, Props}

import scala.collection.mutable.ArrayBuffer
import java.io._

object StatusLogger {
  final case class QueueSnapshot(
      elapse: Long, // nanoseconds since the start of the simulation
      in: Int,
      out: Int,
      current: Int,
      existing: Int,
      creating: Int,
      averageLatency: Double
  )

  final case class ActivationRecord(
      elapse: Long, // nanoseconds since the start of the simulation
      timeToServe: Long, // nanoseconds of waiting in the queue
      timeOfInvoke: Long // nanoseconds of invocation
  )

  def props(outPath: String): Props = Props(new StatusLogger(outPath))
}

class StatusLogger(outDir: String) extends Actor with ActorLogging {
  import StatusLogger._

  private var snapshots = ArrayBuffer.empty[QueueSnapshot]
  private var records = ArrayBuffer.empty[ActivationRecord]

  override def receive: Receive = {
    case s: QueueSnapshot =>
      println(s"""in: ${s.in}, out: ${s.out}, current: ${s.current}
           |, existing: ${s.existing}, creating: ${s.creating},
           |, avgLatency: ${s.averageLatency}
         """.stripMargin.replaceAll("\n", ""))
      snapshots += s
    case r: ActivationRecord =>
      records += r
  }

  override def postStop(): Unit = {
    val dir = new File(outDir)
    if (dir.isFile) {
      log.error(s"the output directory $outDir is already used by a file")
    } else {
      if (!dir.exists()) {
        dir.mkdirs()
      }
      writeSnapshots(dir)
      writeActivations(dir)
    }
  }

  private def writeSnapshots(dir: File): Unit = {
    val file = new File(dir, "snapshots.csv")
    log.info(s"write queue snapshots to ${file.getPath}")

    val writer = new BufferedWriter(new FileWriter(file))
    writer.write("elapse,in,out,current,existing,creating,averageLatency\n")
    snapshots foreach { s =>
      writer.write(
        s"${s.elapse},${s.in},${s.out},${s.current},${s.existing},${s.creating},${s.averageLatency}\n")
    }

    writer.close()
  }

  private def writeActivations(dir: File): Unit = {
    val file = new File(dir, "activations.csv")
    log.info(s"write activation records to ${file.getPath}")

    val writer = new BufferedWriter(new FileWriter(file))
    writer.write("elapse,timeToServe,timeOfInvoke\n")
    records foreach { r =>
      writer.write(s"${r.elapse},${r.timeToServe},${r.timeOfInvoke}\n")
    }

    writer.close()
  }
}

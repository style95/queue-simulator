package style95

import akka.actor.{Actor, Props}

import scala.collection.mutable.ArrayBuffer
import java.io._

object StatusLogger {
  final case class Status(elapse: Long,
                          in: Int,
                          out: Int,
                          current: Int,
                          existing: Int,
                          creating: Int,
                          averageLatency: Double)

  def props(outPath: String): Props = Props(new StatusLogger(outPath))
}

class StatusLogger(outPath: String) extends Actor {
  import StatusLogger._

  private var entries = ArrayBuffer.empty[Status]

  override def receive: Receive = {
    case s: Status =>
      println(s"""in: ${s.in}, out: ${s.out}, current: ${s.current}
           |, existing: ${s.existing}, creating: ${s.creating},
           |, avgLatency: ${s.averageLatency}
         """.stripMargin.replaceAll("\n", ""))
      entries += s
  }

  override def postStop(): Unit = {
    println(s"write status log to $outPath")
    val writer = new BufferedWriter(new FileWriter(new File(outPath)))

    writer.write("elapse,in,out,current,existing,creating,averageLatency\n")
    entries foreach { s =>
      writer.write(
        s"${s.elapse},${s.in},${s.out},${s.current},${s.existing},${s.creating},${s.averageLatency}\n")
    }

    writer.close()
  }
}

import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import scala.concurrent._
import scala.concurrent.duration.Duration
import java.io._
import java.security.{MessageDigest, DigestInputStream}

case class FileFinder(dir: String, calculator: ActorRef) {

  private def findFiles(path: File) {
    if (!path.canRead()) {
      println(s"Non-readable: ${path}")
      return
    }

    if (path.isDirectory)
      path.listFiles foreach findFiles
    else
      calculator ! path
  }

  def run = {
    findFiles(new File(dir))
    calculator ! None
  }
}

class CalcWorker(workerId: Int) extends Actor {
  private def calcMD5(path: File): String = {
    val BUF_SIZE = 0x10000
    val buffer = new Array[Byte](BUF_SIZE)
    val digest = MessageDigest.getInstance("MD5")
    val dis = new DigestInputStream(new FileInputStream(path), digest)
    try {
      while (dis.read(buffer) != -1) {}
    } finally {
      dis.close()
    }
    digest.digest.map{ "%02x".format(_) }.mkString
  }

  private def show(path: File) {
    val md5 = calcMD5(path)
    println(s"$md5  $path")
  }

  def receive = {
    case path: Any =>
      show(path.asInstanceOf[File])
      sender ! workerId
  }
}

class Calculator(numWorkers: Int) extends Actor {
  private val workers = for (i <- 1 to numWorkers) yield {
    context.system.actorOf(Props(classOf[CalcWorker], i), s"worker$i")
  }

  private var idx = 0
  private var terminateFlag = false
  private var numWaitReply = 0

  private def terminateIfAllDone: Unit = {
    if (!terminateFlag)
      return
    if (numWaitReply > 0)
      return
    context.system.terminate
  }

  def receive = {
    case None =>
      terminateFlag = true
      terminateIfAllDone
    case workerId: Int =>
      numWaitReply = numWaitReply - 1
      terminateIfAllDone
    case path: Any =>
      workers(idx) ! path.asInstanceOf[File]
      idx = (idx + 1) % numWorkers
      numWaitReply = numWaitReply + 1
  }
}

case class Args(args: Array[String]) {
  var numCalculators = 1
  var targetDir = ""

  var parser = (arg: String) => println(s"Unknown option: $arg")
  args foreach {
    case "-d" => parser = (arg: String) => targetDir = arg
    case "-c" => parser = (arg: String) => numCalculators = arg.toInt
    case a => parser(a)
  }

  def unary_! = targetDir == ""

  def show = {
    println(s"Targt directory : $targetDir")
    println(s"# of calculators: $numCalculators")
  }
}

case class HashPotate(args: Args) {

  def run = {
    val system = ActorSystem("actor-system")
    val calcProp = Props(classOf[Calculator], args.numCalculators)
    val calculator = system.actorOf(calcProp, "calculator")
    val fileFinder = FileFinder(args.targetDir, calculator)
    fileFinder.run
    Await.ready(system.whenTerminated, Duration.Inf)
  }
}

object Main {
  def main(_args: Array[String]) {
    val args = Args(_args)
    if (!args) {
      System.err.println("You need a directory")
      sys.exit(-1)
    }
    args.show
    val hashPotate = HashPotate(args)
    hashPotate.run
  }
}

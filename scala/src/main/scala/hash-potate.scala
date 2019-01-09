import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import scala.concurrent._
import scala.concurrent.duration.Duration
import java.io._
import java.security.{MessageDigest, DigestInputStream}

case class FileFinder(dir: String, calculator: ActorRef) {

  def findFiles(path: File) {
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

class Calculator extends Actor {
  def calcMD5(path: String): String = {
    val BUF_SIZE = 0x10000
    val buffer = new Array[Byte](BUF_SIZE)
    val digest = MessageDigest.getInstance("MD5")
    val dis = new DigestInputStream(new FileInputStream(new File(path)), digest)
    try {
      while (dis.read(buffer) != -1) {}
    } finally {
      dis.close()
    }
    digest.digest.map{ "%02x".format(_) }.mkString
  }

  def show(path: String) {
    val md5 = calcMD5(path)
    println(s"$md5  $path")
  }

  def receive = {
    case None => context.system.terminate
    case path: Any => show(path.toString)
  }
}

case class HashPotate(dir: String) {

  def run = {
    val system = ActorSystem("actor-system")
    val calculator = system.actorOf(Props[Calculator], "calculator")
    val fileFinder = FileFinder(dir, calculator)
    fileFinder.run
    Await.ready(system.whenTerminated, Duration.Inf)
  }
}

object Main {
  def main(args: Array[String]) {
    if (args.length < 1) {
      System.err.println("You need a directory")
      sys.exit(-1)
    }
    val dir = args(0)
    println(s"Targt dir: $dir")
    val hashPotate = HashPotate(dir)
    hashPotate.run
  }
}

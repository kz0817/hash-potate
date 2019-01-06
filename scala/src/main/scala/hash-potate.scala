import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import java.io._
import java.security.{MessageDigest, DigestInputStream}

case class FileFinder(dir: String, calculator: ActorRef) {

  def findFiles(path: File) {
    if (path.isDirectory)
      path.listFiles foreach findFiles
    else
      calculator ! path
  }

  def run = findFiles(new File(dir)) 
}

class Calculator extends Actor {

  /*
  def calcMD5(text: String): String = {
    val md = MessageDigest.getInstance("MD5")
    md.digest(text.getBytes).map("%02x".format(_)).mkString
  }*/

  def calcMD5(text: String): String = {
    val BUF_SIZE = 0x10000
    val buffer = new Array[Byte](BUF_SIZE)
    val digest = MessageDigest.getInstance("MD5")
    val dis = new DigestInputStream(new FileInputStream(new File(path)), digest)
    try {
      while (dis.read(buffer) != -1) {}
    } finally {
      dis.close()
    }
    digest.digest
  }

  def show(path: String) {
    val md5 = calcMD5(path)
    println(s"$md5  $path")
  }

  def receive = {
    case path => show(path.toString)
  }
}

case class HashPotate(dir: String) {

  def run = {
    val system = ActorSystem("actor-system")
    val calculator = system.actorOf(Props[Calculator], "calculator")
    val fileFinder = FileFinder(dir, calculator)
    fileFinder.run

    system.terminate
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

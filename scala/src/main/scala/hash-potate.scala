import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import java.io._

case class FileFinder(dir: String, calculator: ActorRef) {

  def findFiles(path: File) {
    if (path.isDirectory)
      path.listFiles foreach findFiles
    else
      calculator ! path
  }

  def run() {
    findFiles(new File(dir))
  }
}

class Calculator extends Actor {
  def receive = {
    case path => println(path)
  }
}

case class HashPotate(dir: String) {

  def run() {
    val system = ActorSystem("actor-system")
    val calculator = system.actorOf(Props[Calculator], "calculator")
    val fileFinder = FileFinder(dir, calculator)
    fileFinder.run()

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
    hashPotate.run()
  }
}

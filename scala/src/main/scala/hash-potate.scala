import java.io._

case class FileFinder(dir: String) {

  def run() {
    /*
    new File(dir).listFiles.map {
      println(_)
    }
    */
    new File(dir).listFiles.flatMap {
      println(_)
    }
  }
}

case class HashPotate(dir: String) {
  val fileFinder = FileFinder(dir)

  def run() {
    fileFinder.run()
  }
}

object Main {
  def main(args: Array[String]) {
    if (args.length < 1) {
      sys.error("You need a directory")
      sys.exit(-1)
    }
    val dir = args(0)
    println(s"Targt dir: $dir")
    val hashPotate = HashPotate(dir)
    hashPotate.run()
  }
}

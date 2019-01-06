import java.io._

case class FileFinder(dir: String) {

  private def findFiles(path: File) {
    if (path.isDirectory)
      path.listFiles foreach findFiles
    else
      println(path)
  }

  def run() {
    findFiles(new File(dir))
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
      System.err.println("You need a directory")
      sys.exit(-1)
    }
    val dir = args(0)
    println(s"Targt dir: $dir")
    val hashPotate = HashPotate(dir)
    hashPotate.run()
  }
}

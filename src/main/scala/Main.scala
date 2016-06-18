package RocketToX
import Chisel._

object RocketToXMain {

  def main(args: Array[String]): Unit = {

    // TODO: Fix for compatibility with normal chisel
    val runArgs = args.slice(1, args.length)

    Chisel.chiselMainTest( runArgs, () => Module(new RocketToX(dataWidth = 64, addrWidth = 11)) ) {
      c => new RocketToXTests(c)
    }

  }

}






package AlpakkaKafka.Model

class ThreadInterrupt extends Runnable {

  var flag = false

  def run() = {
    while (true) {
      flag = false
      val input = scala.io.StdIn.readLine()
      if (input != null) {
        flag = true
        Thread.sleep(1000)
      }
    }
  }
}
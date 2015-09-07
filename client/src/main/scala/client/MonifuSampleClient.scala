package client

import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.Observable
import shared.models.Signal
import scala.scalajs.js
import concurrent.duration._

object MonifuSampleClient extends js.JSApp {
  def main(): Unit = {
    val line1 = new DataConsumer(200.millis, 1274028492832L, doBackPressure = true)
      .collect { case s: Signal => s }
    val line2 = new DataConsumer(200.millis, 9384729038472L, doBackPressure = true)
      .collect { case s: Signal => s }
    val line3 = new DataConsumer(200.millis, -2938472934842L, doBackPressure = false)
      .collect { case s: Signal => s }
    val line4 = new DataConsumer(200.millis, -9826395057397L, doBackPressure = false)
      .collect { case s: Signal => s }

    Observable.combineLatest(line1, line2, line3, line4)
      .subscribe(new Graph("lineChart"))
  }
}

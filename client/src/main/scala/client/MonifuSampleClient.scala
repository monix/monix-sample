package client

import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.Observable
import shared.models.Signal
import scala.scalajs.js
import concurrent.duration._

object MonifuSampleClient extends js.JSApp {
  def main(): Unit = {
    val line1 = new DataConsumer(200.millis, 1274028492832L)
      .collect { case s: Signal => s }
    val line2 = new DataConsumer(200.millis, 9384729038472L)
      .collect { case s: Signal => s }
    val line3 = new DataConsumer(200.millis, -2938472934842L)
      .collect { case s: Signal => s }

    Observable.combineLatest(line1, line2, line3)
      .subscribe(new Graph("lineChart"))
  }
}

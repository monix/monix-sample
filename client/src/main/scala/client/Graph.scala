package client

import monifu.reactive.Ack.Continue
import monifu.reactive.{Ack, Observer}
import shared.models.Signal
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal => obj, _}

final class Graph(elementId: String)
  extends Observer[(Signal, Signal, Signal, Signal)] {

  private var chart: js.Dynamic = null

  def initChart(signal: (Signal, Signal, Signal, Signal)) = {
    val (first, second, third, fourth) = signal
    val timestamp = Seq(first.timestamp, second.timestamp, third.timestamp, fourth.timestamp).max / 1000

    global.jQuery(s"#$elementId").epoch(obj(
      "type" -> "time.line",
      "data" -> js.Array(
        obj(
          "label" -> "Series 1",
          "axes" -> js.Array("left", "bottom", "right"),
          "values" -> js.Array(obj(
            "time" -> timestamp,
            "y" -> first.value.toInt
          ))
        ),
        obj(
          "label" -> "Series 2",
          "axes" -> js.Array("left", "bottom", "right"),
          "values" -> js.Array(obj(
            "time" -> timestamp,
            "y" -> second.value.toInt
          ))
        ),
        obj(
          "label" -> "Series 3",
          "axes" -> js.Array("left", "bottom", "right"),
          "values" -> js.Array(obj(
            "time" -> timestamp,
            "y" -> third.value.toInt
          ))
        ),
        obj(
          "label" -> "Series 4",
          "axes" -> js.Array("left", "bottom", "right"),
          "values" -> js.Array(obj(
            "time" -> timestamp,
            "y" -> fourth.value.toInt
          ))
        )
      )
    ))
  }
    
  private def serialize(signal: (Signal, Signal, Signal, Signal)) = {
    val (first, second, third, fourth) = signal
    val timestamp = Seq(first.timestamp, second.timestamp, third.timestamp, fourth.timestamp).max / 1000

    js.Array(
      obj(
        "time" -> timestamp,
        "y" -> first.value.toInt
      ),
      obj(
        "time" -> timestamp,
        "y" -> second.value.toInt
      ),
      obj(
        "time" -> timestamp,
        "y" -> third.value.toInt
      ),
      obj(
        "time" -> timestamp,
        "y" -> fourth.value.toInt
      ))
  }

  def onNext(signal: (Signal, Signal, Signal, Signal)): Future[Ack] = {
    if (chart == null) {
      chart = initChart(signal)
    }
    else {
      chart.push(serialize(signal))
    }


    Continue
  }

  def onComplete(): Unit = ()
  def onError(ex: Throwable): Unit = {
    System.err.println(s"ERROR: $ex")
  }
}

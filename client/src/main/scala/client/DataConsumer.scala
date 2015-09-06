package client

import monifu.reactive.{Observable, Subscriber}
import org.scalajs.dom
import shared.models.{OverflowEvent, Event, Signal}
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js.Dynamic.global

final class DataConsumer(interval: FiniteDuration, seed: Long)
  extends Observable[Event] {

  def onSubscribe(subscriber: Subscriber[Event]): Unit = {
    val host = dom.window.location.host
    val url = s"ws://$host/data-generator?" +
      s"periodMillis=${interval.toMillis}&seed=$seed"

    WebSocketClient(url)
      .collect { case IsEvent(e) => e }
      .onSubscribe(subscriber)
  }

  object IsEvent {
    def unapply(message: String) = {
      val json = global.JSON.parse(message)

      json.event.asInstanceOf[String] match {
        case "point" =>
          Some(Signal(
            value = json.value.asInstanceOf[Number].doubleValue(),
            timestamp = json.timestamp.asInstanceOf[Number].longValue()
          ))
        case "overflow" =>
          Some(OverflowEvent(
            dropped = json.dropped.asInstanceOf[Number].longValue(),
            timestamp = json.timestamp.asInstanceOf[Number].longValue()
          ))
        case "error" =>
          val errorType = json.`type`.asInstanceOf[String]
          val message = json.message.asInstanceOf[String]
          throw new WebSocketClient.Exception(
            s"Server-side error throw - $errorType: $message")
        case _ =>
          None
      }
    }
  }
}

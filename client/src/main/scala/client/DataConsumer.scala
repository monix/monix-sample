package client

import monix.execution.Cancelable
import monix.reactive.Observable
import monix.reactive.OverflowStrategy.DropNew
import monix.reactive.observers.Subscriber
import org.scalajs.dom
import shared.models.{Event, OverflowEvent, Signal}

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js.Dynamic.global

final class DataConsumer(interval: FiniteDuration, seed: Long, doBackPressure: Boolean)
  extends Observable[Event] {

  override def unsafeSubscribeFn(subscriber: Subscriber[Event]): Cancelable = {
    val host = dom.window.location.host
    val protocol = if (dom.document.location.protocol == "https:") "wss:" else "ws:"

    val source = if (doBackPressure) {
      val url = s"$protocol//$host/back-pressured-stream?periodMillis=${interval.toMillis}&seed=$seed"
      BackPressuredWebSocketClient(url)
    }
    else {
      val url = s"$protocol//$host/simple-stream?periodMillis=${interval.toMillis}&seed=$seed"
      SimpleWebSocketClient(url, DropNew(1000))
    }

    source
      .collect { case IsEvent(e) => e }
      .subscribe(subscriber)
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
          throw new BackPressuredWebSocketClient.Exception(
            s"Server-side error throw - $errorType: $message")
        case _ =>
          None
      }
    }
  }
}

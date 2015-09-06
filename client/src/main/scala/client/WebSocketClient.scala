package client

import monifu.reactive.{Observable, Observer, Subscriber}
import org.reactivestreams.Subscription
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}
import scala.util.control.NonFatal

final class WebSocketClient(url: String)
  extends Observable[String] {

  def onSubscribe(subscriber: Subscriber[String]): Unit = {
    import subscriber.scheduler
    val downstream = Observer.toReactiveSubscriber(subscriber)
    val webSocket = new WebSocket(url)

    webSocket.onopen = (event: Event) => {
      downstream.onSubscribe(new Subscription {
        def cancel(): Unit = webSocket.close()
        def request(n: Long): Unit = {
          webSocket.send(n.toString)
        }
      })
    }

    webSocket.onerror = (event: ErrorEvent) => {
      downstream.onError(WebSocketClient.Exception(event.message))
      try webSocket.close() catch { case NonFatal(_) => () }
    }

    webSocket.onclose = (event: CloseEvent) => {
      downstream.onComplete()
    }

    webSocket.onmessage = (event: MessageEvent) => {
      downstream.onNext(event.data.asInstanceOf[String])
    }
  }
}

object WebSocketClient {
  def apply(url: String): WebSocketClient = {
    new WebSocketClient(url)
  }

  case class Exception(msg: String) extends RuntimeException(msg)
}

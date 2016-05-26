package client

import monix.execution.Ack.Stop
import monix.execution.{Ack, Cancelable}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, OverflowStrategy}
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

/** This `Observable` is a simple variant that does not communicate
  * using the Reactive Streams back-pressure protocol, like the one
  * implemented by [[BackPressuredWebSocketClient]].
  *
  * Instead this connection uses a client-side buffer than can overflow. We
  * can control what happens on overflow if our subscribers are too slow
  * (like dropping events). But the disadvantage is that the server can't
  * find out about it.
  */
final class SimpleWebSocketClient private (url: String, os: OverflowStrategy.Synchronous[String])
  extends Observable[String] { self =>

  /** An `Observable` that upon subscription will open a
    * buffered web-socket connection.
    */
  private val channel: Observable[String] =
    // This `create` builder is safer to use (versus unsafeCreate), because
    // the injected subscriber is going to be buffered and you don't
    // have to know details about the back-pressure protocol in order to use it.
    Observable.create[String](os) { downstream =>
      // Reusing this in 2 places
      def closeConnection(webSocket: WebSocket): Unit = {
        Utils.log(s"Closing connection to $url")
        if (webSocket != null && webSocket.readyState <= 1)
          try webSocket.close() catch {
            case _: Throwable => ()
          }
      }

      try {
        Utils.log(s"Connecting to $url")
        val webSocket = new WebSocket(url)

        // Not doing anything on open
        webSocket.onopen = (event: Event) => ()

        webSocket.onerror = (event: ErrorEvent) => {
          // If error, signal it and it will be the last message
          downstream.onError(BackPressuredWebSocketClient.Exception(event.message))
        }
        webSocket.onclose = (event: CloseEvent) => {
          // If close, signal it and it will be the last message
          downstream.onComplete()
        }
        webSocket.onmessage = (event: MessageEvent) => {
          // Signal next event as usual, but we need to catch
          // Stop acknowledgements. But given this is a synchronous
          // (buffered) subscriber, it's a simple if statement.
          val ack = downstream.onNext(event.data.asInstanceOf[String])
          if (ack == Stop) closeConnection(webSocket)
        }

        Cancelable(() => closeConnection(webSocket))
      } catch {
        case NonFatal(ex) =>
          // Normally this could be a race condition, meaning that we aren't allowed to
          // send `onError` twice and at this point we have no way of knowing if `onError`
          // already happened, but this right here is fine, for one because this is Javascript,
          // but also because the `downstream` is protected by a concurrent buffer.
          downstream.onError(ex)
          Cancelable.empty
      }
    }

  override def unsafeSubscribeFn(subscriber: Subscriber[String]): Cancelable =
    channel.unsafeSubscribeFn(new Subscriber[String] {
      val scheduler = subscriber.scheduler

      def onNext(elem: String): Future[Ack] =
        subscriber.onNext(elem)

      def onError(ex: Throwable): Unit = {
        scheduler.reportFailure(ex)
        // Retry connection in a couple of secs
        self
          .delaySubscription(3.seconds)
          .unsafeSubscribeFn(subscriber)
      }

      def onComplete(): Unit = {
        // Retry connection in a couple of secs
        self
          .delaySubscription(3.seconds)
          .unsafeSubscribeFn(subscriber)
      }
    })
}

object SimpleWebSocketClient {
  def apply(url: String, os: OverflowStrategy.Synchronous[String]): SimpleWebSocketClient = {
    new SimpleWebSocketClient(url, os)
  }

  case class Exception(msg: String) extends RuntimeException(msg)

}

package client

import monix.execution.{Ack, Cancelable}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, Observer}
import org.reactivestreams.Subscription
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.control.NonFatal

/** Creates a connection to a server endpoint that supports the
  * reactive-streams back-pressure protocol.
  *
  * This means that the client will keep sending a number to
  * the server, signaling that it is ready for consuming more
  * events and so the server is allowed to send a maximum number
  * of events equal to the request, or a completion event.
  *
  * Here we really are making use of the Reactive Streams protocol
  * (specified at http://www.reactive-streams.org/), because
  * sending a `request(n)` over the wire will be more efficient
  * than sending acknowledgements for each processed event.
  *
  * By doing this back-pressure, it means that the buffers are fixed
  * and that the server is able to detect slow clients and do something
  * about it, like the server can start dropping messages on the floor.
  *
  * This example is maybe a little too low-level, implementing
  * an `Observable` from scratch, with no helpers. This shouldn't
  * be done normally, as you have to know what you're doing.
  */
final class BackPressuredWebSocketClient private (url: String)
  extends Observable[String] { self =>

  /** An `Observable` that upon subscription will open a
    * back-pressured web-socket connection.
    */
  private val channel: Observable[String] =
    // You can see the "unsafe" word here, and it's no joke. It means that
    // you shouldn't do this unless you know what you're doing.
    Observable.unsafeCreate { subscriber =>
      // Reusing this in 2 places
      def closeConnection(webSocket: WebSocket): Unit = {
        Utils.log(s"Closing connection to $url")
        if (webSocket != null && webSocket.readyState <= 1)
          try webSocket.close() catch {
            case _: Throwable => ()
          }
      }

      // Converting a Monix Subscriber to a Reactive Subscriber.
      // Will make sure to consume the stream by processing and
      // requesting events in batches, applying back-pressure and
      // respecting its contract.
      val downstream = Subscriber.toReactiveSubscriber(subscriber)

      try {
        // Opening a WebSocket connection using Javascript's API
        Utils.log(s"Connecting to $url")
        val webSocket = new WebSocket(url)

        // Registering on WebSocket's callbacks
        webSocket.onopen = (event: Event) => {
          // When we've got that connection, we can send the `onSubscribe`
          // that basically describes what happens on `request(n)` and
          // on `closeConnection` (according to the Reactive Streams protocol)
          downstream.onSubscribe(new Subscription {
            def cancel(): Unit =
              closeConnection(webSocket)
            def request(n: Long): Unit = {
              webSocket.send(n.toString)
            }
          })
        }
        webSocket.onerror = (event: ErrorEvent) => {
          // If error, signal it and it will be the last message
          downstream.onError(BackPressuredWebSocketClient.Exception(event.message))
        }
        webSocket.onclose = (event: CloseEvent) => {
          // If close, signal it and it will be the last message
          downstream.onComplete()
        }
        webSocket.onmessage = (event: MessageEvent) => {
          // Signal next event as usual
          downstream.onNext(event.data.asInstanceOf[String])
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

  /** Subscribers to the WebSocket with a retry logic that keeps
    * it connected or re-connecting no matter what (until canceled).
    *
    * This is actually equivalent to usage of `Observable.unsafeCreate`,
    * same as above.
    */
  override def unsafeSubscribeFn(subscriber: Subscriber[String]): Cancelable = {
    import subscriber.scheduler

    channel.unsafeSubscribeFn(new Observer[String] {
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
}

object BackPressuredWebSocketClient {
  def apply(url: String): BackPressuredWebSocketClient = {
    new BackPressuredWebSocketClient(url)
  }

  case class Exception(msg: String) extends RuntimeException(msg)
}

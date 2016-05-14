package client

import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, Observer, OverflowStrategy, Pipe}
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal


final class SimpleWebSocketClient private(url: String, os: OverflowStrategy.Synchronous[String])
  extends Observable[String] {
  self =>

  private def createChannel()(implicit s: Scheduler): Observable[String] = {

    var webSocket: WebSocket = null
    try {
      Utils.log(s"Connecting to $url")
      webSocket = new WebSocket(url)
    } catch {
      case NonFatal(ex) =>
        Observable.raiseError(ex)
    }

    def closeConnection(): Unit = {
      if (webSocket != null && webSocket.readyState <= 1)
        try webSocket.close() catch {
          case _: Throwable => ()
        }
    }

    try {
      val (in, out) = Pipe.publish[String].concurrent(os)
      webSocket.onopen = (event: Event) => ()

      webSocket.onerror = (event: ErrorEvent) => {
        in.onError(BackPressuredWebSocketClient.Exception(event.message))
      }

      webSocket.onclose = (event: CloseEvent) => {
        in.onComplete()
      }

      webSocket.onmessage = (event: MessageEvent) => {
        in.onNext(event.data.asInstanceOf[String])
      }

      out.doOnCancel(closeConnection())
    } catch {
      case NonFatal(ex) =>
        Observable.raiseError(ex)
    }
  }

  override def unsafeSubscribeFn(subscriber: Subscriber[String]): Cancelable = {
    import subscriber.scheduler

    val subscription: Cancelable =
      createChannel().unsafeSubscribeFn(new Observer[String] {
        def onNext(elem: String): Future[Ack] =
          subscriber.onNext(elem)

        def onError(ex: Throwable): Unit = {
          subscription.cancel()
          scheduler.reportFailure(ex)
          // retry connection in a couple of secs
          self
            .delaySubscription(3.seconds)
            .unsafeSubscribeFn(subscriber)
        }

        def onComplete(): Unit = {
          subscription.cancel()
          // retry connection in a couple of secs
          self
            .delaySubscription(3.seconds)
            .unsafeSubscribeFn(subscriber)
        }
      })

    subscription
  }
}

object SimpleWebSocketClient {
  def apply(url: String, os: OverflowStrategy.Synchronous[String]): SimpleWebSocketClient = {
    new SimpleWebSocketClient(url, os)
  }

  case class Exception(msg: String) extends RuntimeException(msg)

}

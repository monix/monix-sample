package client

import monix.execution.cancelables.BooleanCancelable
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, Observer}
import org.reactivestreams.Subscription
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}

import scala.concurrent._
import scala.concurrent.duration._


final class BackPressuredWebSocketClient private(url: String)
  extends Observable[String] {
  self =>

  private def createChannel(webSocket: WebSocket) =
    new Observable[String] {
      override def unsafeSubscribeFn(subscriber: Subscriber[String]): Cancelable = {
        import subscriber.scheduler
        val downstream = Observer.toReactiveSubscriber(subscriber)

        try {
          webSocket.onopen = (event: Event) => {
            downstream.onSubscribe(new Subscription {
              def cancel(): Unit =
                closeConnection(webSocket)

              def request(n: Long): Unit = {
                webSocket.send(n.toString)
              }
            })
          }

          webSocket.onerror = (event: ErrorEvent) => {
            downstream.onError(BackPressuredWebSocketClient.Exception(event.message))
          }

          webSocket.onclose = (event: CloseEvent) => {
            downstream.onComplete()
          }

          webSocket.onmessage = (event: MessageEvent) => {
            downstream.onNext(event.data.asInstanceOf[String])
          }
        } catch {
          case ex: Throwable =>
            downstream.onError(ex)
        }

        BooleanCancelable()
      }
    }

  private def closeConnection(webSocket: WebSocket)(implicit s: Scheduler): Unit = {
    if (webSocket != null && webSocket.readyState <= 1)
      try webSocket.close() catch {
        case _: Throwable => ()
      }
  }

  override def unsafeSubscribeFn(subscriber: Subscriber[String]): Cancelable = {
    import subscriber.scheduler

    var webSocket: WebSocket = null
    val channel = try {
      Utils.log(s"Connecting to $url")
      webSocket = new WebSocket(url)
      createChannel(webSocket)
    }
    catch {
      case ex: Throwable =>
        Observable.error(ex)
    }

    val source = channel.throttleWithTimeout(5.seconds)
      .doOnCancel(closeConnection(webSocket))

    source.subscribe(new Observer[String] {
      def onNext(elem: String): Future[Ack] =
        subscriber.onNext(elem)

      def onError(ex: Throwable): Unit = {
        closeConnection(webSocket)
        scheduler.reportFailure(ex)
        // retry connection in a couple of secs
        self.delaySubscription(3.seconds)
          .subscribe(subscriber)
      }

      def onComplete(): Unit = {
        closeConnection(webSocket)
        // retry connection in a couple of secs
        self.delaySubscription(3.seconds)
          .subscribe(subscriber)
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

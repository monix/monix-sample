package client

import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, Observer}
import org.reactivestreams.Subscription
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.control.NonFatal


final class BackPressuredWebSocketClient private(url: String)
  extends Observable[String] {
  self =>

  private def createChannel() =
    new Observable[String] {
      override def unsafeSubscribeFn(subscriber: Subscriber[String]): Cancelable = {
        import subscriber.scheduler

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

        val downstream = Observer.toReactiveSubscriber(subscriber)

        try {
          webSocket.onopen = (event: Event) => {
            downstream.onSubscribe(new Subscription {
              def cancel(): Unit =
                closeConnection()

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
          case NonFatal(ex) =>
            downstream.onError(ex)
        }

        Cancelable(closeConnection)
      }
    }

  override def unsafeSubscribeFn(subscriber: Subscriber[String]): Cancelable = {
    import subscriber.scheduler

    val subscription: Cancelable =
      createChannel().subscribe(new Observer[String] {
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

object BackPressuredWebSocketClient {
  def apply(url: String): BackPressuredWebSocketClient = {
    new BackPressuredWebSocketClient(url)
  }

  case class Exception(msg: String) extends RuntimeException(msg)

}

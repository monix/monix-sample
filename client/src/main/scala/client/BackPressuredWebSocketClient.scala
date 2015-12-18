package client

import monifu.concurrent.Scheduler
import monifu.reactive.{Ack, Observable, Observer, Subscriber}
import org.reactivestreams.Subscription
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}
import scala.concurrent._
import concurrent.duration._


final class BackPressuredWebSocketClient private (url: String)
  extends Observable[String] { self =>

  private def createChannel(webSocket: WebSocket) =
    Observable.create[String] { subscriber =>
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
      }
      catch {
        case ex: Throwable =>
          downstream.onError(ex)
      }
    }

  private def closeConnection(webSocket: WebSocket)(implicit s: Scheduler): Unit = {
    if (webSocket != null && webSocket.readyState <= 1)
      try webSocket.close() catch { case _: Throwable => () }
  }

  def onSubscribe(subscriber: Subscriber[String]): Unit = {
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

    val source = channel.timeout(5.seconds)
      .doOnCanceled(closeConnection(webSocket))

    source.onSubscribe(new Observer[String] {
      def onNext(elem: String): Future[Ack] =
        subscriber.onNext(elem)

      def onError(ex: Throwable): Unit = {
        closeConnection(webSocket)
        scheduler.reportFailure(ex)
        // retry connection in a couple of secs
        self.delaySubscription(3.seconds)
          .onSubscribe(subscriber)
      }

      def onComplete(): Unit = {
        closeConnection(webSocket)
        // retry connection in a couple of secs
        self.delaySubscription(3.seconds)
          .onSubscribe(subscriber)
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

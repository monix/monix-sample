package client

import monix.execution.{Ack, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, Observer, OverflowStrategy}
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}

import scala.concurrent.Future
import scala.concurrent.duration._


final class SimpleWebSocketClient private
  (url: String, os: OverflowStrategy.Synchronous)
  extends Observable[String] { self =>

  private def createChannel(webSocket: WebSocket)(implicit s: Scheduler) = {
    try {
      val channel = PublishChannel[String](os)
      webSocket.onopen = (event: Event) => ()

      webSocket.onerror = (event: ErrorEvent) => {
        channel.pushError(BackPressuredWebSocketClient.Exception(event.message))
      }

      webSocket.onclose = (event: CloseEvent) => {
        channel.pushComplete()
      }

      webSocket.onmessage = (event: MessageEvent) => {
        channel.pushNext(event.data.asInstanceOf[String])
      }

      channel
    }
    catch {
      case ex: Throwable =>
        Observable.error(ex)
    }
  }

  private def closeConnection(webSocket: WebSocket)(implicit s: Scheduler): Unit = {
    if (webSocket != null && webSocket.readyState <= 1)
      try webSocket.close() catch { case _: Throwable => () }
  }

  def onSubscribe(subscriber: Subscriber[String]): Unit = {
    import subscriber.scheduler

    var webSocket: WebSocket = null
    val channel: Observable[String] = try {
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

object SimpleWebSocketClient {
  def apply(url: String, os: OverflowStrategy.Synchronous): SimpleWebSocketClient = {
    new SimpleWebSocketClient(url, os)
  }

  case class Exception(msg: String) extends RuntimeException(msg)
}

package client

import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, Observer, OverflowStrategy, Pipe}
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, WebSocket}

import scala.concurrent.Future
import scala.concurrent.duration._


final class SimpleWebSocketClient private(url: String, os: OverflowStrategy.Synchronous[String])
  extends Observable[String] { self =>

  private def createChannel(webSocket: WebSocket)(implicit s: Scheduler): Observable[String] = {
    try {
      val channel = Pipe.publish[String].concurrent(os)
      webSocket.onopen = (event: Event) => ()

      webSocket.onerror = (event: ErrorEvent) => {
        channel._1.onError(BackPressuredWebSocketClient.Exception(event.message))
      }

      webSocket.onclose = (event: CloseEvent) => {
        channel._1.onComplete()
      }

      webSocket.onmessage = (event: MessageEvent) => {
        channel._1.onNext(event.data.asInstanceOf[String])
      }

      channel._2
    }
    catch {
      case ex: Throwable =>
        Observable.raiseError(ex)
    }
  }

  private def closeConnection(webSocket: WebSocket)(implicit s: Scheduler): Unit = {
    if (webSocket != null && webSocket.readyState <= 1)
      try webSocket.close() catch { case _: Throwable => () }
  }

  override def unsafeSubscribeFn(subscriber: Subscriber[String]): Cancelable = {
    import subscriber.scheduler

    var webSocket: WebSocket = null
    val channel: Observable[String] = try {
      Utils.log(s"Connecting to $url")
      webSocket = new WebSocket(url)
      createChannel(webSocket)
    }
    catch {
      case ex: Throwable =>
        Observable.raiseError(ex)
    }

    val source = channel.dropByTimespan(1.second)
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
  def apply(url: String, os: OverflowStrategy.Synchronous[String]): SimpleWebSocketClient = {
    new SimpleWebSocketClient(url, os)
  }

  case class Exception(msg: String) extends RuntimeException(msg)
}

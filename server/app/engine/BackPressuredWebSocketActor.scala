package engine

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.scalalogging.LazyLogging
import engine.BackPressuredWebSocketActor._
import monifu.concurrent.Scheduler
import monifu.reactive.Observable
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.streams.SingleAssignmentSubscription
import org.reactivestreams.{Subscriber, Subscription}
import play.api.libs.json.{Writes, JsObject, JsValue, Json}
import shared.models.Event
import scala.concurrent.duration._


class BackPressuredWebSocketActor[T <: Event : Writes]
  (producer: Observable[T], out: ActorRef)(implicit s: Scheduler)
  extends Actor with LazyLogging {

  def receive: Receive = {
    case Request(nr) =>
      subscription.request(nr)
  }

  private[this] val subscription =
    SingleAssignmentSubscription()

  def now(): Long =
    System.currentTimeMillis()

  override def preStart(): Unit = {
    super.preStart()

    val source = {
      val initial = Observable.unit(initMessage(now()))
      val obs = initial ++ producer.map(x => Json.toJson(x))
      val timeout = obs.debounceRepeated(5.seconds).map(_ => keepAliveMessage(now()))

      Observable.merge(obs, timeout)
        .whileBusyBuffer(DropOld(100), nr => onOverflow(nr, now()))
    }

    source.toReactive.subscribe(new Subscriber[JsValue] {
      def onSubscribe(s: Subscription): Unit = {
        subscription := s
      }

      def onNext(json: JsValue): Unit = {
        out ! json
      }

      def onError(t: Throwable): Unit = {
        logger.warn(s"Error while serving a web-socket stream", t)
        out ! Json.obj(
          "event" -> "error",
          "type" -> t.getClass.getName,
          "message" -> t.getMessage,
          "timestamp" -> now())

        context.stop(self)
      }

      def onComplete(): Unit = {
        out ! Json.obj("event" -> "complete", "timestamp" -> now())
        context.stop(self)
      }
    })
  }

  override def postStop(): Unit = {
    subscription.cancel()
    super.postStop()
  }
}

object BackPressuredWebSocketActor {
  /** Utility for quickly creating a `Props` */
  def props[T <: Event : Writes](producer: Observable[T], out: ActorRef)
    (implicit s: Scheduler): Props = {

    Props(new BackPressuredWebSocketActor(producer, out))
  }

  /**
   * For pattern matching request events.
   */
  object Request {
    def unapply(value: Any): Option[Long] =
      value match {
        case str: String =>
          str.trim match {
            case IsInteger(integer) =>
              try Some(integer.toLong).filter(_ > 0) catch {
                case _: NumberFormatException =>
                  None
              }
            case _ =>
              None
          }
        case number: Int =>
          Some(number.toLong)
        case number: Long =>
          Some(number)
        case _ =>
          None
      }

    val IsInteger = """^([-+]?\d+)$""".r
  }

  /**
   * Builds an overflow notification.
   */
  def onOverflow(dropped: Long, now: Long): JsObject =
    Json.obj(
      "event" -> "overflow",
      "dropped" -> dropped,
      "timestamp" -> now
    )

  /**
   * Keep-alive message.
   */
  def keepAliveMessage(now: Long) = {
    Json.obj("event" -> "keep-alive", "timestamp" -> now)
  }

  /**
   * Keep-alive message.
   */
  def initMessage(now: Long) = {
    Json.obj("event" -> "init", "timestamp" -> now)
  }
}


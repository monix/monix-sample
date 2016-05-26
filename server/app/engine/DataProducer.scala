package engine

import util.Random
import monix.execution.{Cancelable, FutureUtils}
import monix.reactive.Observable
import monix.reactive.observers.Subscriber
import shared.models.Signal

import scala.concurrent.duration._

final class DataProducer(interval: FiniteDuration, seed: Long)
  extends Observable[Signal] {

  private case class State(x: Int, y: Int, ts: Long)

  override def unsafeSubscribeFn(subscriber: Subscriber[Signal]): Cancelable = {
    import subscriber.{scheduler => s}

    val random = Observable
      .fromStateAction(Random.intInRange(-20, 20))(s.currentTimeMillis() + seed)
      .flatMap { x => Observable.now(x).delaySubscription(interval) }

    val generator = random.scan(Signal(0, s.currentTimeMillis())) {
      case (Signal(value, _), rnd) =>
        val next = value + rnd
        Signal(next, s.currentTimeMillis())
    }

    generator
      .drop(1)
      .unsafeSubscribeFn(subscriber)
  }
}

package engine

import monix.execution.Cancelable
import monix.reactive.Observable
import monix.reactive.observers.Subscriber
import shared.models.Signal
import util.Random
import scala.concurrent.duration._

final class DataProducer(interval: FiniteDuration, seed: Long)
  extends Observable[Signal] {

  override def unsafeSubscribeFn(subscriber: Subscriber[Signal]): Cancelable = {
    import subscriber.{scheduler => s}

    val random = Observable
      .fromStateAction(Random.intInRange(-20, 20))(s.currentTimeMillis() + seed)
      .flatMap { x => Observable.now(x).delaySubscription(interval) }

    val generator = random.scan(Signal(0, s.currentTimeMillis())) {
      case (Signal(value, _), rnd) =>
        Signal(value + rnd, s.currentTimeMillis())
    }

    generator
      .drop(1)
      .unsafeSubscribeFn(subscriber)
  }
}

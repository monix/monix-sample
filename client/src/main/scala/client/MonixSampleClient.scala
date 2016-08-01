package client

import monix.execution.Ack.Continue
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{MulticastStrategy, Observable}
import monix.reactive.subjects.{ConcurrentSubject, PublishSubject}
import shared.models.Signal

import scala.concurrent._
import scala.concurrent.duration._
import scala.scalajs.js
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.timers._

object MonixSampleClient extends js.JSApp {

  def timeout(ms: Double): Future[Unit] = {
    val p = Promise[Unit]()
    setTimeout(ms) {
      p.success {}
      ()
    }
    p.future
  }

  def main(): Unit = {
    val line1 = new DataConsumer(200.millis, 1274028492832L, doBackPressure = true)
      .collect { case s: Signal => s }
    val line2 = new DataConsumer(200.millis, 9384729038472L, doBackPressure = true)
      .collect { case s: Signal => s }
    val line3 = new DataConsumer(200.millis, -2938472934842L, doBackPressure = false)
      .collect { case s: Signal => s }
    val line4 = new DataConsumer(200.millis, -9826395057397L, doBackPressure = false)
      .collect { case s: Signal => s }

    Observable
      .combineLatest4(line1, line2, line3, line4)
      .subscribe(new Graph("lineChart"))



    //    for { i <- 1 to 100} {
    //             source.onNext(i)
    //            }
    //    Observable.interval(1.second).take(10).subscribe( v => source.onNext(v))


    val source: ConcurrentSubject[Long,Long] = ConcurrentSubject(MulticastStrategy.Publish)


    Future { source.onNext(1)}
    Future { source.onNext(2)}
    Future { source.onNext(3)}
    Future { source.onNext(4)}
    Future { source.onNext(5)}

    source.subscribe { v => Future {
      println("st " + v)
    }.flatMap { _ =>


        timeout(10*100).map { _ =>

          println("end " + v)
        }
    }.flatMap (_ => Continue) }
  }
}

package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import engine.{BackPressuredWebSocketActor, DataProducer, SimpleWebSocketActor}
import monix.execution.Scheduler.Implicits.global
import play.api.Environment
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import scala.concurrent.duration._

class ApplicationController()
  (implicit env: Environment, as: ActorSystem, m: Materializer)
  extends Controller with JSONFormats {

  implicit val messageFlowTransformer =
    MessageFlowTransformer.jsonMessageFlowTransformer[JsValue, JsValue]

  def index = Action {
    Ok(views.html.index(env))
  }

  def backPressuredStream(periodMillis: Int, seed: Long) =
    WebSocket.accept[JsValue, JsValue] { request =>
      val obs = new DataProducer(periodMillis.millis, seed)
      ActorFlow.actorRef(out => BackPressuredWebSocketActor.props(obs, out))
    }

  def simpleStream(periodMillis: Int, seed: Long) =
    WebSocket.accept[JsValue, JsValue] { request =>
      val obs = new DataProducer(periodMillis.millis, seed)
      ActorFlow.actorRef(out => SimpleWebSocketActor.props(obs, out))
    }
}

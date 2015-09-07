package controllers

import monifu.concurrent.Implicits.globalScheduler
import engine.{SimpleWebSocketActor, BackPressuredWebSocketActor, DataProducer}
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.Play.current
import concurrent.duration._

object Application extends Controller with JSONFormats {
  def index = Action {
    Ok(views.html.index())
  }

  def backPressuredStream(periodMillis: Int, seed: Long) =
    WebSocket.acceptWithActor[String, JsValue] { req => out =>
      val obs = new DataProducer(periodMillis.millis, seed)
      BackPressuredWebSocketActor.props(obs, out)
    }

  def simpleStream(periodMillis: Int, seed: Long) =
    WebSocket.acceptWithActor[String, JsValue] { req => out =>
      val obs = new DataProducer(periodMillis.millis, seed)
      SimpleWebSocketActor.props(obs, out)
    }
}

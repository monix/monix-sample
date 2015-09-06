package controllers

import monifu.concurrent.Implicits.globalScheduler
import engine.{WebSocketActor, DataProducer}
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.Play.current
import concurrent.duration._

object Application extends Controller with JSONFormats {
  def index = Action {
    Ok(views.html.index())
  }

  def dataGenerator(periodMillis: Int, seed: Long) =
    WebSocket.acceptWithActor[String, JsValue] { req => out =>
      val obs = new DataProducer(periodMillis.millis, seed)
      WebSocketActor.props(obs, out)
    }
}

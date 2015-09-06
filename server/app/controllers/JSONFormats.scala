package controllers

import play.api.libs.json._
import shared.models.Signal

trait JSONFormats {
  private val defaultPointFormat = Json.format[Signal]

  implicit val pointFormat = new Format[Signal] {
    def reads(json: JsValue): JsResult[Signal] =
      (json \ "event").validate[String].flatMap {
        case "point" =>
          defaultPointFormat.reads(json)
        case _ =>
          JsError(JsPath \ "event", s"Event is not `point`")
      }

    def writes(o: Signal): JsValue =
      Json.obj("event" -> o.event) ++
        defaultPointFormat.writes(o).as[JsObject]
  }
}

package config

import controllers.{ApplicationController, Assets}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n.I18nComponents
import router.Routes

class SampleApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    new SampleComponents(context).application
  }
}

class SampleComponents(context: Context) extends BuiltInComponentsFromContext(context)
  with I18nComponents {

  lazy val router = new Routes(httpErrorHandler, applicationController, assets)

  lazy val assets = new Assets(httpErrorHandler)
  lazy val applicationController = new ApplicationController()(environment, actorSystem, materializer)
}
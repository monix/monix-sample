package client

import scala.scalajs.js
import scala.scalajs.js.Dynamic._

object Utils {
  /** Logs a message to the browser's console.
    *
    * Not sure if I have to do this, maybe println
    * would work just fine, but you can't really assume
    * `console.log` is present.
    */
  def log(message: String) = {
    val canLog = !js.isUndefined(global.console) &&
      !js.isUndefined(global.console.log)
    if (canLog) global.console.log(message)
  }
}

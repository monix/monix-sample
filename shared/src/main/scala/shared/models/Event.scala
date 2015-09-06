package shared.models

/**
 * An interface for values that get streamed between
 * the client and server, signaling their type and
 * the timestamp at which they happened.
 */
trait Event {
  def event: String
  def timestamp: Long
}

case class OverflowEvent(dropped: Long, timestamp: Long)
  extends Event {

  val event = "overflow"
}

case class Signal(value: Double, timestamp: Long)
  extends Event {

  val event = "point"
}
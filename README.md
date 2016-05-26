# Monix Client/Server Sample

Sample of a mixed client/server application demonstrating
the streaming of values over web-socket, both simple and with 
back-pressure applied (by means of the [Reactive Streams](http://www.reactive-streams.org/)
protocol), using [Monix](https://github.com/monixio/monix) for both
the server and the client ;-)

See: **[monix-sample.herokuapp.com](http://monix-sample.herokuapp.com)**

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/monixio/monix?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Overview of the code:

- the server-side values generator is 
  in [engine.DataProducer](server/app/engine/DataProducer.scala)
- the server-side WebSocket channel that implements the back-pressure
  protocol as defined by [Reactive Streams](http://www.reactive-streams.org/) 
  is handled by 
  [engine.BackPressuredWebSocketActor](server/app/engine/BackPressuredWebSocketActor.scala) (using
  Play's [WebSocket support](https://www.playframework.com/documentation/2.4.x/ScalaWebSockets#Handling-WebSockets-with-actors))
  and the client-side consumer is 
  [client.BackPressuredWebSocketClient](client/src/main/scala/client/BackPressuredWebSocketClient.scala)
- the server-side WebSocket channel that doesn't do back-pressure is
  [engine.SimpleWebSocketActor](server/app/engine/SimpleWebSocketActor.scala)
  while the client-side consumer is
  [client.SimpleWebSocketClient](client/src/main/scala/client/SimpleWebSocketClient.scala)
- the type-safe observable that listens to a server web-socket connection
  generating signals is in 
  [client.DataConsumer](client/src/main/scala/client/DataConsumer.scala)
- the integration with Epoch, our charting library, is in
  [client.Graph](client/src/main/scala/client/Graph.scala)
  
NOTES:

- we are exposing 2 versions of webSocket connections, one that is back-pressured
  as a matter of the server-side protocol and one that is not
- both versions are protected by buffers that start dropping events in case
  the client is too slow, but the difference is that for the back-pressured 
  version the buffer is being maintained server-side
- by applying back-pressure in the protocol, the server is informed of the 
  rate at which the client can consume and thus there is no risk for the
  server in case we've got clients that are too slow; on the other hand for the
  simple version the server can be crashed on clients that are too slow in 
  receiving their events, as that actor's mailbox is unbounded
- the back-pressured version needs server-side cooperation / implementation and
  is thus more difficult to develop
  
In order to develop and execute the project locally:
```
sbt run
```

The project is based on the cool 
[play-with-scalajs-example](https://github.com/vmunier/play-with-scalajs-example)
template by @vmunier, so you get the auto-reload coolness of Play in combination
with Scala.js.

~;Enjoy

# Monifu Client/Server Sample

Sample of a mixed client/server application demonstrating
the streaming of values over web-socket, both simple and with 
back-pressure applied (by means of the [Reactive Streams](http://www.reactive-streams.org/)
protocol).

See: **[monifu-sample.herokuapp.com](http://monifu-sample.herokuapp.com)**

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

~;Enjoy

# Monifu Client/Server Sample

Sample of a mixed client/server application demonstrating
the streaming of values over web-socket, while doing back-pressure.

See: **[monifu-sample.herokuapp.com](http://monifu-sample.herokuapp.com)**

Overview of the code:

- the server-side values generator is 
  in [engine.DataProducer](server/app/engine/DataProducer.scala)
- the server-side WebSocket channel that pushes those values is in
  [engine.WebSocketActor](server/app/engine/WebSocketActor.scala) (using
  Play's [WebSocket support](https://www.playframework.com/documentation/2.4.x/ScalaWebSockets#Handling-WebSockets-with-actors))
- the generic client-side WebSocket consumer is in 
  [client.WebSocketClient](client/src/main/scala/client/WebSocketActor.scala)
- the type-safe observable that listens to a server web-socket connection
  generating signals is in 
  [client.DataConsumer](client/src/main/scala/client/DataConsumer.scala)
- the integration with Epoch, our charting library, is in
  [client.Graph](client/src/main/scala/client/Graph.scala)

~;Enjoy

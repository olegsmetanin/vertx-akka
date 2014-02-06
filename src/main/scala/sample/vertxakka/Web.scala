package sample.vertxakka

import org.vertx.scala.core.Vertx
import org.vertx.java.core.impl.DefaultVertx
import org.vertx.scala.core.buffer.Buffer
import org.vertx.scala.core.eventbus.Message
import org.vertx.scala.core.sockjs.{SockJSServer}
import org.vertx.scala.core.http.{HttpServer}

import org.vertx.scala.core.json.JsonObject
import play.api.libs.json.Json
import sample.vertxakka.AkkaSystem._


object Web {

  def start = {

  }

  val vertx: Vertx = Vertx(new DefaultVertx)

  val eventBus = vertx.eventBus

  eventBus.registerLocalHandler("send", (msg: Message[String]) => {
    val index = msg.body().indexOf(",")
    socketActor ! Send(msg.body().substring(0, index), msg.body().substring(index))
  })

  val server: HttpServer = vertx.createHttpServer()

  server.requestHandler {
    req =>
      if (req.path().equals("/")) req.response.sendFile("index.html")
  }

  server.websocketHandler(sock => {
    if (sock.path().equals("/ws")) {

      val id = java.util.UUID.randomUUID.toString

      socketActor ! ConnectWebSoket(id, sock)

      sock.endHandler {
        socketActor ! Disconnect(id)
      }

      sock.dataHandler {
        data: Buffer =>
          val jsValue = Json parse data.toString()
          val address: String = (jsValue \ "address").as[String]
          if (address == "") {
            sock.writeTextFrame("self:" + id)
          } else {
            socketActor ! Send(address, "send from " + address)
          }
      }
    }
  })


  val sockJSServer: SockJSServer = vertx.createSockJSServer(server)
  val config: JsonObject = new JsonObject().putString("prefix", "/sock")

  sockJSServer.installApp(config,
    (sock) => {
      val id = java.util.UUID.randomUUID.toString

      socketActor ! ConnectSoketJS(id, sock)

      sock.endHandler {
        socketActor ! Disconnect(id)
      }

      sock.dataHandler {
        data: Buffer =>
          val jsValue = Json parse data.toString()
          val address: String = (jsValue \ "address").as[String]
          if (address == "") {
            sock.write(Buffer("self:" + id))
          } else {
            socketActor ! Send(address, "send from " + address)
          }

      }
    }
  )

  server.listen(8080)
  println("Webserver started")

}

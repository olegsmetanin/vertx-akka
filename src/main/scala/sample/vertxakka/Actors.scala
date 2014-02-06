package sample.vertxakka

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import akka.actor.Actor
import org.vertx.scala.core.sockjs.SockJSSocket
import org.vertx.scala.core.http.ServerWebSocket
import org.vertx.scala.core.buffer.Buffer


case class ConnectWebSoket(id: String, sock: ServerWebSocket)

case class ConnectSoketJS(id: String, sock: SockJSSocket)

case class Send(id: String, msg: String)

case class Disconnect(id: String)

class SocketActor extends Actor {

  val sockStore = new ConcurrentLinkedHashMap.Builder[String, (Option[SockJSSocket], Option[ServerWebSocket])]
    .initialCapacity(1000000)
    .maximumWeightedCapacity(2000000)
    .build()

  def receive = {
    case ConnectWebSoket(id: String, sock: ServerWebSocket) => {
      sockStore.put(id, (None, Some(sock)))
    }
    case ConnectSoketJS(id: String, sock: SockJSSocket) => {
      sockStore.put(id, (Some(sock), None))
    }
    case Send(id: String, msg: String) => {
      sockStore.get(id.trim) match {
        case (_, Some(sock)) => sock.writeTextFrame(msg)
        case (Some(sock), _) => sock.write(Buffer(msg))
        case _ =>
      }
    }
    case Disconnect(id: String) => {
      sockStore.remove(id)
    }
  }

}
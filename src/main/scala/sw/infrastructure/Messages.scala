package sw.infrastructure

import com.github.nscala_time.time.Imports._
import org.vertx.scala.core.http.ServerWebSocket
import org.vertx.scala.core.sockjs.SockJSSocket
import akka.actor.ActorRef

case class WorkSystemRegistration(module: String, version: String)

case class Film(code: String, title: String, did: Int, date_prod: LocalDate, kind: String, len: Period)


case class RegisterWorkSystem(name: String, version: String)

case class RegisterWebSystem()

case class RegisterSocketSystem()

case class UnregisterWorkSystem()

case class RegisterRouterSystem()

case class UnregisterRouterSystem()

case class ConnectWebSoket(id: String, sock: ServerWebSocket)

case class ConnectSoketJS(id: String, sock: SockJSSocket)

case class SocketRecieve(id: String, headers: Map[String, Set[String]], msg: String)

case class SocketSend(id: String, msg: String)

case class Broadcast(msg: String)

case class BroadcastUser(sessionid: String)

case class Disconnect(id: String)

case class WorkSystemInfo(module: String, version: String, actorRef: ActorRef)

case class Request(headers: Map[String, Set[String]], params: Map[String, Set[String]], body: String)

case class ChunkedRequest(socketId: String, body: String)

case class SocketRequest(socketId: String, body: String)

case class Work(socketId: String, payload: String)

case class ChunkedResponse(socketId: String, body: String)

case class SocketResponse(socketId: String, body: String)

case class Response(body: String)

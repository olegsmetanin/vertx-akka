package sw.infrastructure

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.Predef._
import scala.Some

import akka.util.Timeout
import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberUp}

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap

import org.vertx.scala.core._
import org.vertx.scala.core.http.ServerWebSocket
import org.vertx.scala.core.sockjs.SockJSSocket
import org.vertx.scala.core.buffer.Buffer
import org.vertx.scala.core.sockjs.{SockJSServer}
import org.vertx.scala.core.http.{HttpServer}
import org.vertx.scala.core.json.JsonObject
import org.vertx.scala.platform.Verticle
import org.vertx.java.core.VertxFactory
import org.vertx.java.core.impl.DefaultVertx


object SocketActor {

  val name = "socketActor"
}

class SocketActor extends Actor {

  implicit val timeout: Timeout = 10.second // for the actor 'asks'

  import context.dispatcher

  // ExecutionContext for the futures and scheduler

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)

  var jobCounter = 0

  var workers = IndexedSeq.empty[WorkSystemInfo]

  val sockStore = new ConcurrentLinkedHashMap.Builder[String, (Option[SockJSSocket], Option[ServerWebSocket])]
    .initialCapacity(1000000)
    .maximumWeightedCapacity(2000000)
    .build()

  val sessionsStore = new ConcurrentLinkedHashMap.Builder[String, Set[String]]
    .initialCapacity(1000000)
    .maximumWeightedCapacity(2000000)
    .build()

  def receive = {

    case ConnectWebSoket(id: String, sock: ServerWebSocket) => {
      //supervisor ! Broadcast("web","Joined "+id)
      sockStore.put(id, (None, Some(sock)))
      //val
      //sessionsStore.put(session, Option(sessionsStore.get(session)).getOrElse(Set()) + id)
    }
    case ConnectSoketJS(id: String, sock: SockJSSocket) => {
      //println("Connect sockjs id:" + id)
      sockStore.put(id, (Some(sock), None))
    }

    case Disconnect(id: String) => {
      sockStore.remove(id)
      //sessionsStore.remove()
    }

    case SocketSend(id: String, msg: String) => {
      sockStore.get(id.trim) match {
        case (_, Some(sock)) => sock.writeTextFrame(msg)
        case (Some(sock), _) => sock.write(Buffer(msg))
        case _ =>
      }
    }

    case s@SocketRecieve(id: String, headers: Map[String, Set[String]], msg: String) => {
      if (workers.size > 0) {
        jobCounter += 1
        val worker: ActorRef = workers(jobCounter % workers.size).actorRef
        worker ! s
      } else {
        self ! SocketSend(id, JSONResponse.NOWORKERS)
      }
    }

    case BroadcastUser(sessionid: String) => {

    }

    case Broadcast(msg: String) => {
      sockStore.values.foreach(_ match {
        case (_, Some(sock)) => sock.writeTextFrame(msg)
        case (Some(sock), _) => sock.write(Buffer(msg))
        case _ =>
      })
    }

    case RegisterWorkSystem(name: String, version: String) if !workers.contains(WorkSystemInfo(name, version, sender)) => {
      println(Console.BLUE + "Socket: worker " + sender.toString() + " " + name + " " + version + " REGISTRED" + Console.RESET)
      val newWorkSystem = WorkSystemInfo(name, version, sender)
      if (!workers.contains(newWorkSystem)) {
        context watch sender
        workers = workers :+ newWorkSystem
      }
      sender ! RegisterSocketSystem
    }

    case Terminated(actor) => {
      println(Console.RED + "Socket: actor " + actor.toString() + " is Terminated" + Console.RESET)
      workers = workers.filterNot(_.actorRef == actor)
    }


  }

}

class SocketVerticle(socketActor: ActorRef, port: Int) extends Verticle {

  override def start() = {

  }
}


object SocketSystem extends GenSystem {

  val role = "socketsystem"

  def apply()(implicit system: ActorSystem) = new SocketSystem(system)

}

class SocketSystem(system: ActorSystem) {

  val config = system.settings.config

  val socketActor = system.actorOf(Props[SocketActor], name = SocketActor.name)

  val port = if (config.hasPath("socket.port")) config.getInt("socket.port") else Utils.freePort

  val vertx: Vertx = Vertx(new DefaultVertx) //VertxFactory.newVertx() // Vertx(new DefaultVertx)
  //val pm = PlatformLocator.factory.createPlatformManager

  val server: HttpServer = vertx.createHttpServer()

  implicit def Multimap2MapSet(in_mmap: MultiMap): Map[String, Set[String]] = {
    in_mmap.entries().map(el => (el.getKey, el.getValue)).toList.groupBy(_._1).map {
      case (k, v) => (k, v.map(_._2).toSet)
    }
  }

  server.websocketHandler(sock => {
    if (sock.path().equals("/bus/ws")) {

      val id = java.util.UUID.randomUUID.toString

      socketActor ! ConnectWebSoket(id, sock)

      sock.endHandler {
        socketActor ! Disconnect(id)
      }

      sock.dataHandler {
        data: Buffer =>
          socketActor ! SocketRecieve(id, sock.headers, data.toString())

      }
    }
  })

  val sockJSServer: SockJSServer = vertx.createSockJSServer(server)

  val sockConfig: JsonObject = new JsonObject().putString("prefix", "/bus/sock")

  sockJSServer.installApp(sockConfig,
    (sock) => {
      val id = java.util.UUID.randomUUID.toString

      socketActor ! ConnectSoketJS(id, sock)

      sock.endHandler {
        socketActor ! Disconnect(id)
      }

      sock.dataHandler {
        data: Buffer =>
          socketActor ! SocketRecieve(id, sock.asJava.headers(), data.toString())

      }
    }
  )

  server.listen(port)

  println(s"WebSystem started on port $port")

}
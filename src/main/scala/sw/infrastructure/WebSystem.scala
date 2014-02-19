package sw.infrastructure

import scala.Predef._
import scala.language.implicitConversions
import scala.concurrent.duration._

import akka.actor._
import akka.pattern._
import akka.cluster.ClusterEvent.{MemberUp}
import akka.cluster.{Cluster}
import akka.io.IO
import akka.util.Timeout

import spray.can.Http
import spray.http._
import spray.http.HttpMethods._
import spray.http.MediaTypes._


object WebActor {
  val name = "webActor"
}

class WebActor extends Actor with ActorLogging {

  implicit val timeout: Timeout = 10.second // for the actor 'asks'

  import context.dispatcher

  // ExecutionContext for the futures and scheduler

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)

  var jobCounter = 0

  var workers = IndexedSeq.empty[WorkSystemInfo]

  def index = {
    val source = scala.io.Source.fromFile("web/index.html")
    val byteArray = source.map(_.toByte).toArray
    source.close()
    byteArray
  }

  def mainjs = {
    val source = scala.io.Source.fromFile("web/main.js")
    val byteArray = source.map(_.toByte).toArray
    source.close()
    byteArray
  }

  def receive = {

    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      sender ! HttpResponse(entity = HttpEntity(`text/html`, index))

    case HttpRequest(GET, Uri.Path("/main.js"), _, _, _) =>
      sender ! HttpResponse(entity = HttpEntity(`application/javascript`, mainjs))

    case msg@HttpRequest(POST, Uri.Path("/api"), _, _, _) => {
      //println(msg.headers.toString)
      if (workers.size > 0) {
        jobCounter += 1
        val worker: ActorRef = workers(jobCounter % workers.size).actorRef
        (worker ? msg).mapTo[HttpResponse] pipeTo (sender)
      } else {
        sender ! JSONHTTPResponse.NOWORKERS
      }
    }

    case _: HttpRequest => sender ! HttpResponse(status = 404, entity = "Unknown resource!")

    case Timedout(HttpRequest(method, uri, _, _, _)) =>
      sender ! HttpResponse(
        status = 500,
        entity = "The " + method + " request to '" + uri + "' has timed out..."
      )

    case RegisterWorkSystem(name: String, version: String) if !workers.contains(WorkSystemInfo(name, version, sender)) => {
      println(Console.BLUE + "Web: worker " + sender.toString() + " " + name + " " + version + " REGISTRED" + Console.RESET)
      val newWorkSystem = WorkSystemInfo(name, version, sender)
      if (!workers.contains(newWorkSystem)) {
        context watch sender
        workers = workers :+ newWorkSystem
      }
    }

    case Terminated(actor) => {
      println(Console.RED + "Web: actor " + actor.toString() + " is Terminated" + Console.RESET)
      workers = workers.filterNot(_.actorRef == actor)
    }

  }

}

object WebSystem extends GenSystem {

  val role = "websystem"

  def apply()(implicit system: ActorSystem) = new WebSystem(system)

}

class WebSystem(akkaSystem: ActorSystem) {

  implicit val system: ActorSystem = akkaSystem

  val config = system.settings.config

  val webActor = system.actorOf(Props[WebActor], name = WebActor.name)

  val host = "0.0.0.0"

  val port = if (config.hasPath("web.port")) config.getInt("web.port") else Utils.freePort

  IO(Http) ! Http.Bind(webActor, interface = host, port = port)

  println(s"WebSystem started on port $port")

}
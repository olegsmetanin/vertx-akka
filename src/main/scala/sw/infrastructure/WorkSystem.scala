package sw.infrastructure

import akka.actor._
import akka.cluster.{Member, MemberStatus, Cluster}
import spray.http._
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import spray.http.HttpRequest

import spray.http.HttpResponse
import akka.actor.RootActorPath
import akka.cluster.ClusterEvent.CurrentClusterState

import akka.cluster.ClusterEvent.MemberUp
import sw.db._
import sw.api.Responser


object WorkActor {

  val name = "workActor"

}

class WorkActor extends Actor {


  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)

  var socketWorkers = IndexedSeq.empty[ActorRef]

  def receive = {

    case MemberUp(member) => {
      println(Console.YELLOW + "Worker: new member " + member.address + member.roles + " is up" + Console.RESET)
      register(member)
    }

    case state: CurrentClusterState => state.members.filter(_.status == MemberStatus.Up) foreach register

    case RegisterSocketSystem => {
      if (!socketWorkers.contains(sender)) {
        context watch sender
        socketWorkers = socketWorkers :+ sender
      }
    }

    case req@HttpRequest(POST, Uri.Path("/api"), _, _, _) => {

      import scala.concurrent.ExecutionContext.Implicits.global
      import DAO._

      // important to save sender!!!
      val snd = sender

      implicit val WorkActor = this
      Responser.generate(req.entity.data.asString, None).onComplete {
        s =>
          snd ! HttpResponse(entity = HttpEntity(`application/json`, s.get))
      }
    }

    case SocketRecieve(id: String, headers: Map[String, Set[String]], msg: String) => {
      import scala.concurrent.ExecutionContext.Implicits.global
      import DAO._

      // important to save sender!!!
      val snd = sender
      val sid = id

      implicit val WorkActor = this
      Responser.generate(msg, Some(id)).onComplete {
        s =>
          snd ! SocketSend(sid, s.get)
      }
    }

  }

  def register(member: Member): Unit = {
    if (member.hasRole(WebSystem.role))
      context.actorSelection(RootActorPath(member.address) / "user" / WebActor.name) ! RegisterWorkSystem(WorkSystem.name, WorkSystem.version)
    if (member.hasRole(SocketSystem.role))
      context.actorSelection(RootActorPath(member.address) / "user" / SocketActor.name) ! RegisterWorkSystem(WorkSystem.name, WorkSystem.version)
  }
}

object WorkSystem extends GenSystem {
  val role = "worksystem"
  val name = "worksystem"
  val version = "1"

  def apply()(implicit system: ActorSystem) = new WorkSystem(system)
}

class WorkSystem(system: ActorSystem) {
  val workActor = system.actorOf(Props[WorkActor], name = WorkActor.name)
}

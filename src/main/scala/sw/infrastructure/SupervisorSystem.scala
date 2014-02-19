package sw.infrastructure


import akka.cluster.{Cluster}
import akka.cluster.ClusterEvent._
import akka.actor._
import akka.cluster.ClusterEvent.MemberRemoved
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.UnreachableMember

object SupervisorActor {

  val name = "supervisorActor"

}

class SupervisorActor extends Actor {

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) => {
      println("Supervisor: member " + member.address + " " + member.roles + " is Up")
    }
    case UnreachableMember(member) =>
      println("Supervisor: Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      println("Supervisor: Member is Removed: {} after {}",
        member.address, previousStatus)
    case _: MemberEvent => // ignore
  }

}

object SupervisorSystem extends GenSystem {

  val role = "supervisor"

  def apply()(implicit system: ActorSystem) = new SupervisorSystem(system)

}

class SupervisorSystem(system: ActorSystem) {

  val supervisorActor = system.actorOf(Props[SupervisorActor], name = SupervisorActor.name)

  val systemAddress = system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress.toString
  println("SupervisorSystem started")
  println( s"""
  Run websystem: ./sbt -Dakka.remote.netty.tcp.hostname=_hostname -Dakka.cluster.seed-nodes.0="$systemAddress" -Dvertx.port=_port \"run websystem\"
  """)

}

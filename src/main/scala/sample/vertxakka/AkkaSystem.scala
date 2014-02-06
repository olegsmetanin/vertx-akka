package sample.vertxakka

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}
import scala.concurrent.duration._

object AkkaSystem {

  def start = {

  }

  import scala.collection.JavaConversions._

  val ips = for {
    ip <- java.net.NetworkInterface.getNetworkInterfaces.map(_.getInetAddresses).flatten
    // Some problem with ipv6
    if ip.isInstanceOf[java.net.Inet4Address]
    if ip.getHostAddress != "127.0.0.1"
  } yield {
    ip.getHostAddress
  }

  val ip = ips.next

  val customConf = ConfigFactory.parseString(config(ip, 2552))
  val system = ActorSystem("actorSystem", ConfigFactory.load(customConf))
  val socketActor = system.actorOf(Props(classOf[SocketActor]), "socketActor")


  println("Started ActorSystem")

  import system.dispatcher

  system.scheduler.scheduleOnce(1.second) {
  }

  def config(ip: String, port: Int) = {
    s"""
     akka {

       actor {
         provider = "akka.remote.RemoteActorRefProvider"
       }

       remote {
         netty.tcp {
           hostname = "$ip"
           port = $port
         }
       }


     }

    metrics-dispatcher {
      mailbox-type = "sample.remote.pingpong.MetricsMailboxType"
    }


     """
  }
}
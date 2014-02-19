package sw.infrastructure

import akka.actor.{ActorSystem}

import java.net.ServerSocket


object Vars {
  val sessionID = "SessionID"
}





trait GenSystem {
  val role: String

  def apply()(implicit system: ActorSystem)
}

object Utils {
  def freePort = {
    val server = new ServerSocket(0)
    val port = server.getLocalPort()
    server.close
    port
  }
}


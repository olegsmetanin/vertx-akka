/**
 * Copyright (C) 2014 Oleg Smetanin
 */
package sample.vertxakka

import org.vertx.scala.core._
import org.vertx.java.core.{Vertx => JVertx}


import org.vertx.scala.core.Handler
import org.vertx.scala.core.buffer.Buffer
import org.vertx.scala.core.http.{HttpServerRequest, HttpServer}
import org.vertx.scala.core.json._
import org.vertx.java.core.sockjs._


import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import org.vertx.scala.core.sockjs.{SockJSSocket, SockJSServer}
import org.vertx.java.core.impl.DefaultVertx


import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap


import play.api.libs.json.Json

import org.vertx.scala.core.eventbus._


object Starter {

  val vertx: Vertx = Vertx(new DefaultVertx)

  val eventBus = vertx.eventBus

  private val userSockStore = new ConcurrentLinkedHashMap.Builder[String, List[String]]
    .initialCapacity(1000000)
    .maximumWeightedCapacity(2000000)
    .build()
  private val sessionSockStore = new ConcurrentLinkedHashMap.Builder[String, SockJSSocket]
    .initialCapacity(1000000)
    .maximumWeightedCapacity(2000000)
    .build()

  def main(args: Array[String]): Unit = {
    //startActorSystem()


    val server: HttpServer = vertx.createHttpServer()

    server.requestHandler {
      req =>

        req.response.sendFile("index.html")

    }

    server.listen(8080)



    val sockJSInstance: HttpServer = {

      val httpServer: HttpServer = vertx.createHttpServer()
      val sockJSServer: SockJSServer = vertx.createSockJSServer(httpServer)
      val sockjsPrefix = "/ws" //app.configuration.getString("sockjsplugin.prefix").getOrElse("/ws")
      val config: JsonObject = new JsonObject().putString("prefix", sockjsPrefix)


      sockJSServer.installApp(config,
        (sock) => {
          val id = java.util.UUID.randomUUID.toString

          sessionSockStore.put(id, sock)

          eventBus.registerLocalHandler("send-" + id, (msg: Message[String]) => {
            sock.write(Buffer(msg.body()))
          })

          sock.endHandler {
            sessionSockStore.remove(id)
          }

          sock.dataHandler {
            data: Buffer =>
              val jsValue = Json parse data.toString()
              val address: String = (jsValue \ "address").as[String]
              if (address == "") {
                sock.write(Buffer("self:" + id))
              } else {
                eventBus.publish("send-" + address, "send from " + address)
              }

          }

        }
      )

      httpServer.listen(8081)

    }

    readLine()

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

  //val ip = java.net.InetAddress.getLocalHost().getHostAddress() - return 127.0.0.1

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

  def startActorSystem(): Unit = {

    val customConf = ConfigFactory.parseString(config(ip, 2552))

    val system = ActorSystem("PingSystem", ConfigFactory.load(customConf))
    //val pingActor = system.actorOf(Props(classOf[PingActor]).withDispatcher("metrics-dispatcher"), "pingActor")

    println("Started ActorSystem")

    import system.dispatcher
    system.scheduler.scheduleOnce(1.second) {
      //println("Start warming")
      //pingActor ! PingThemAll
    }

  }

}

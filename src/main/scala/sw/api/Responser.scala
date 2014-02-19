package sw.api


import scala.concurrent.Future
import sw.infrastructure._
import sw.db.{DAO, DBs}
import play.api.libs.json.Json
import sw.infrastructure.Film
import scala.Some
import sw.infrastructure.Broadcast
import scala.concurrent.duration._


object Responser {

  def generate(requestBody: String, socketId: Option[String])(implicit workActor: WorkActor): Future[String] = {

    import scala.concurrent.ExecutionContext.Implicits.global
    import DAO._

    val json = Json.parse(requestBody)

    val method = (json \ "method").asOpt[String]

    method match {

      case Some("simpleRequest") => {
        Future(JSONResponse.result("Simple response"))
      }


      case Some("sql") => {
        (json \ "sql").asOpt[String] match {
          case Some(sql) => {
            DBs("saas")
              .sendQuery(sql)
              .asListOf[Film]
              .map {
              f =>
                val films = f.toString
                s"""
                  {
                     "result": "$films"
                  }
                """
            }
          }
          case _ => {
            Future(JSONResponse.error("No sql command"))
          }
        }
      }


      case Some("broadcast") => {
        val message = (json \ "message").toString
        workActor.socketWorkers.foreach(_ ! Broadcast(
          s"""
          {
             "type" : "broadcast",
             "message": $message
          }
          """))

        Future(JSONResponse.result("OK"))
      }

      case Some("stream") => {
        val snd = workActor.context.sender

        val res = for {
          socket <- socketId
          stream <- (json \ "streamid").asOpt[String]
        } yield {

          val periodic = workActor.context.system.scheduler.schedule(1.second, 1.second) {
            val time = System.currentTimeMillis()
            snd ! SocketSend(socket,
              s"""
                {
                   "type" : "stream",
                   "streamid": "$stream",
                   "message": "Stream message $time"
                }
              """)
          }

          val stopper = workActor.context.system.scheduler.scheduleOnce(10.second) {
            periodic.cancel

            snd ! SocketSend(socket,
              s"""
                {
                   "type" : "stream",
                   "streamid": "$stream",
                   "end": "true"
                }
              """)
          }

          JSONResponse.result("OK")
        }

        Future(res.getOrElse(JSONResponse.error("No streamId or request coming not from socket")))
      }

      case _ => {
        Future(JSONResponse.error("No such method"))
      }
    }


  }

}

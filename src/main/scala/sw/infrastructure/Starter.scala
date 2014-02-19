package sw.infrastructure

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import scala.collection.JavaConversions._


object Starter extends App {

  args.toList match {
    case configsNames: List[String] => {

      import scala.reflect.runtime.universe

      val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

      val configs = configsNames.map(ConfigFactory.load(_))

      val classes = configs.map(cfg => cfg.getString("loadclass"))

      val systems = classes.map {
        cls =>

          val module = runtimeMirror.staticModule(cls)

          val obj = runtimeMirror.reflectModule(module)

          obj.instance.asInstanceOf[GenSystem]

      }.toList

      val roles = systems.map(sys => sys.role)

      val config = ConfigFactory
        .parseString("akka.cluster.roles = [" + roles.mkString(",") + "]")
        .withFallback(configs.foldLeft(ConfigFactory.empty())((c, f) => c.withFallback(f)))

      implicit val system = ActorSystem("ClusterSystem", config)

      systems foreach (_())
    }

    case _ => println("nonvalid arg")
  }


}
package sw.es

import com.sksamuel.elastic4s.ElasticClient

object ES {

  val connection = ElasticClient.remote("localhost", 9200)

}

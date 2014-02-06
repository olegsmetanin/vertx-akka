/**
 * Copyright (C) 2014 Oleg Smetanin
 */
package sample.vertxakka

import AkkaSystem._
import Web._

object Starter {
  def main(args: Array[String]): Unit = {
    AkkaSystem.start
    Web.start

    readLine()

  }


}

name := "vertx-akka"

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-remote" % "2.3.0-RC1",
  "com.googlecode.concurrentlinkedhashmap"  %   "concurrentlinkedhashmap-lru" % "1.4",
  "io.vertx" % "vertx-core" % "2.1M5",
  "io.vertx" % "lang-scala" % "0.3.0",
  "io.netty" % "netty-buffer" % "4.0.15.Final",
  "com.typesafe.play" % "play-json_2.10" % "2.2.1"
)

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

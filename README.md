vertx-akka
==========

Start haproxy

haproxy -f haproxy.cfg

Run all in one ActorSystem

```
./sbt -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.0.1:2552" -Dweb.port=8080 -Dsocket.port=8081 "run supervisor websystem socketsystem worksystem"
```

Run separate supervisor

```
./sbt -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.0.1:2552" "run supervisor"
```

Run separate websystem

```
./sbt -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.0.1:2552" -Dweb.port=8080 "run websystem"
```

Run separate socketsystem

```
./sbt -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.0.1:2552" -Dsocket.port=8081 "run socketsystem"
```


Run separate worker

```
./sbt -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.cluster.seed-nodes.0="akka.tcp://ClusterSystem@127.0.0.1:2552" "run worksystem"
```
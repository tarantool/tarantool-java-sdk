---
title: Architecture
---

## TDBCluster

The main interface for working with a [TarantoolDB](https://www.tarantool.io/en/tarantooldb/doc/latest/) cluster within `testcontainers` is `TDBCluster`:

```puml
@startuml
!theme plain
top to bottom direction
skinparam linetype ortho

interface AutoCloseable << interface >> {
  + close(): void
}
interface Startable << interface >> {
  + stop(): void
  + getDependencies(): Set<Startable>
  + close(): void
  + start(): void
}
interface TDBCluster << interface >> {
  + clusterName(): String
  + tcmContainer(): TCMContainer
  + storages(): Map<String, TarantoolContainer<?>>
  + etcdContainer(): EtcdContainer
  + routers(): Map<String, TarantoolContainer<?>>
  + restart(long, TimeUnit): void
  + nodes(): Map<String, TarantoolContainer<?>>
}

Startable      -[#008200,plain]-^  AutoCloseable
TDBCluster     -[#008200,plain]-^  Startable
@enduml
```

## Requirements for Interface Implementations

### TDB Cluster Components

Depending on the `TarantoolDB(TDB)` version, the cluster may contain various components (containers) necessary for its operation:

```puml
@startuml
'https://plantuml.com/component-diagram

rectangle "TDBCluster" {
  rectangle "Tarantool Nodes" as TN {
    rectangle "Routers" {
      database "Router-1" as R1
      database "Router-2" as R2
    }
    rectangle "Replicasets" {
      rectangle "replicaset-1" {
        database "replica-1 (master)" as M1
        database "replica-2" as M1R1
        database "replica-N" as M1RN
      }
      rectangle "replicaset-2" {
        database "replica-1 (master)" as M2
        database "replica-2" as M2R1
        database "replica-N" as M2RN
      }
    }
  }
  rectangle "TarantoolDB 2.x only" {
    database etcd
    node TCM
    note left of TCM
      Used for
      interaction with
      the cluster via UI
      (connection to
      all cluster nodes)
    end note
    note right of etcd {
      Distributed storage
      of cluster configuration and migrations
      of TarantoolDB.
      (connection to
      all cluster nodes)
    }
  }
}

R1 --> M1
R1 --> M2
R2 --> M1
R2 --> M2
M1 --> M1R1
M1 --> M1RN
M2 --> M2R1
M2 --> M2RN

TCM <--> TN
etcd <--> TN
@enduml
```

### Restarting the Cluster

Cluster restart is performed using the `void restart(long delay, TimeUnit unit)` method, where `delay` is the wait duration, and `unit` defines the measurement unit of `delay`. Implementations must ensure that mounted data is preserved when using this method.

### Stopping the Cluster

Stopping the cluster with closing all resources is performed using the `stop()` and `close()` methods:

```java
import org.junit.Test;
import org.testcontainers.containers.tdb.TDBCluster;
import org.testcontainers.utility.DockerImageName;

public class TestClass {

  @Test
  public void method() {
    final DockerImageName image = DockerImageName.parse("tarantooldb:2.2.1");

    try (TDBCluster cluster = new SomeTDBClusterImplementation()) {
      cluster.start();
      cluster.start(); //valid. idempotency

    } // call `close()` method of AutoCloseable interface
  }
}
```

```java

import org.junit.Test;
import org.testcontainers.containers.tdb.TDBCluster;
import org.testcontainers.utility.DockerImageName;

public class TestClass {

  @Test
  public void method() {
    final DockerImageName image = DockerImageName.parse("tarantooldb:2.1.1");

    try (TDBCluster cluster = new SomeTDBClusterImplementation()) {
      cluster.start();

      cluster.stop();
      cluster.stop(); // valid. idempotency
      
      // Uncomment to see the exception
      // cluster.start(); // invalid. Throws `already closed` exception
    }
  }
}
```

### Port Binding

After starting the cluster, each `TDBCluster` component (containers) is assigned a free external port. Implementations must guarantee that the assigned ports will be retained for containers until the `close()` or `stop()` methods are called.

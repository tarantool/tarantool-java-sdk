---
title: TarantoolDB. Архитектура
---

## TDBCluster

Основным интерфейсом для работы с кластером 
[TarantoolDB](https://www.tarantool.io/en/tarantooldb/doc/latest/) в рамках `testcontainers` 
является `TDBCluster`:

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

## Требования к реализациям интерфейса

### Компоненты кластера TDB

В зависимости от версии `TarantoolDB(TDB)` в кластере могут присутствовать различные
компоненты (контейнеры), необходимые для его работы:

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
      Используется для
      взаимодействия с
      кластером через UI
      (подключение ко
      всем узлам кластера)
    end note
    note right of etcd {
      Распределенное хранилище
      конфигурации и миграций
      кластера TarantoolDB.
      (подключение ко
      всем узлам кластера)
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

### Перезапуск кластера

Перезапуск кластера осуществляется с помощью метода `void restart(long delay, TimeUnit unit)`, где
`delay` - длительность ожидания, `unit` - определяет единицу измерения `delay`. Реализации должны
обеспечить сохранение монтируемых данных при использовании этого метода.

### Остановка кластера

Остановка кластера с закрытием всех ресурсов производится с помощью методов `stop()`, `close()`:

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

### Привязка портов

После запуска кластера, каждому компоненту `TDBCluster` (контейнерам) выделяется свободный
внешний порт. Реализации должны гарантировать, что присвоенные порты будут сохранены за контейнерами
до момента вызова методов `close()` или `stop()`.

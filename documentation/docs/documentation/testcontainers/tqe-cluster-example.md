---
title: TQE. Примеры
---

## Примеры использования TQE Testcontainers

### Пример 1. Запуск кластера TQE c одной grpc-точкой

Определим файл с настройками grpc-точки (`simple-grpc.yml`):

```yaml
# порт метрик (обязательный)
core_port: 1111

# адрес grpc-сервера (обязательный)
grpc_listen:
  - uri: 'tcp://0.0.0.0:18182'

# Обозначим, что узел является издателем(grpc-publisher)
publisher:
  enabled: true
  #  настройки подключения к узлам Tarantool
  tarantool:
    user: test-super
    pass: test
    connections:
      # Адреса маршрутизаторов
      routers:
        - "router:3301"

# Обозначим, что также является подписчиком(grpc-consumer)
consumer:
  enabled: true
  tarantool:
    user: test-super
    pass: test
    connections:
      # Список адресов узлов шардов Tarantool
      storage:
        - "master:3301"
```

Определим конфигурацию узлов Tarantool (`simple-queue.yml`):

```yaml
# Credentials
credentials:
  users:
    # Обязательный тестовый пользователь 
    test-super:
      password: 'test'
      roles: [ super ]
    admin:
      password: 'secret-cluster-cookie'
      roles: [ super ]
    replicator:
      password: 'secret'
      roles: [ replication ]
    storage:
      roles: [ sharding ]
      password: storage

# advertise configs for all nodes
iproto:
  advertise:
    peer:
      login: replicator
    sharding:
      login: storage
      password: storage

roles: [ roles.metrics-export ]

# Определим тестовую очередь
roles_cfg:
  app.roles.queue:
    queues:
      - name: test
        deduplication_mode: keep_latest
        disabled_filters_by: [ sharding_key ]
  roles.metrics-export:
    http:
      - listen: 8081
        endpoints:
          - format: prometheus
            path: '/metrics'

groups:
  routers:
    replicasets:
      r-1:
        sharding:
          roles: [ router ]
        # Обязательная роль          
        roles: [ app.roles.api ]
        instances:
          router:
            iproto:
              listen:
                - uri: router:3301
  storages:
    replicasets:
      shard-1:
        replication:
          failover: manual
        sharding:
          roles: [ storage ]
          
        # Обязательная роль
        roles: [ app.roles.queue ]
        leader: master
        instances:
          master:
            iproto:
              listen:
                - uri: master:3301
```

Для того чтобы запустить кластер TQE, воспользуемся следующим кодом:

```java

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.containers.tqe.GrpcContainer;
import org.testcontainers.containers.tqe.TQECluster;
import org.testcontainers.containers.tqe.TQEClusterImpl;
import org.testcontainers.containers.tqe.configuration.FileTQEConfigurator;
import org.testcontainers.containers.tqe.configuration.TQEConfigurator;
import org.testcontainers.utility.DockerImageName;

public class TestClass {

  @Test
  public void test() {
    final Path grpcConfigPath = Paths.get("path/to/simple-grpc.yml");
    final Path queueConfigPath = Paths.get("path/to/simple.queue.yml");
    final DockerImageName image = DockerImageName.parse("tqe-image-name:tag");

    try (TQEConfigurator configurator =
        FileTQEConfigurator.builder(image, queueConfigPath, Collections.singleton(grpcConfigPath));
        TQECluster cluster = new TQEClusterImpl(configurator)
    ) {
      cluster.start();

      // получим grpc точки
      final Map<String, GrpcContainer<?>> grpc = cluster.grpc();
      Assertions.assertEquals(1, grpc.size());

      // получим узлы очереди
      final Map<String, TarantoolContainer<?>> queue = cluster.queue();
      Assertions.assertEquals(1, queue.size());

      // перезапустим кластер
      cluster.restart(1, TimeUnit.SECONDS, 1, TimeUnit.SECONDS);
    }
  }
}
```

### Пример 2. Кластер с отдельными узлами (издатель(publisher)/подписчик(consumer))

Воспользуемся конфигурацией очереди (кластера `Tarantool`) определенную
в `примере 1`. Определим отдельные конфигурации для `grpc-подписчика` и `grpc-издателя`.

Конфигурация для `grpc-издателя` (`simple-grpc-publisher.yml`):

```yaml
# порт метрик (обязательный)
core_port: 1111

# адрес grpc-сервера (обязательный)
grpc_listen:
  - uri: 'tcp://0.0.0.0:18182'

# Обозначим, что узел является издателем(grpc-publisher)
publisher:
  enabled: true
  #  настройки подключения к узлам Tarantool
  tarantool:
    user: test-super
    pass: test
    connections:
      # Адреса маршрутизаторов
      routers:
        - "router:3301"
```

Конфигурация для `grpc-подписчика` (`simple-grpc-consumer.yml`):

```yaml
# порт метрик (обязательный)
core_port: 1111

# адрес grpc-сервера (обязательный)
grpc_listen:
  - uri: 'tcp://0.0.0.0:18182'

# Обозначим, что также является подписчиком(grpc-consumer)
consumer:
  enabled: true
  tarantool:
    user: test-super
    pass: test
    connections:
      # Список адресов узлов шардов Tarantool
      storage:
        - "master:3301"
```

Для запуска кластера воспользуемся следующим кодом:

```java
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.containers.tqe.GrpcContainer;
import org.testcontainers.containers.tqe.TQECluster;
import org.testcontainers.containers.tqe.TQEClusterImpl;
import org.testcontainers.containers.tqe.configuration.FileTQEConfigurator;
import org.testcontainers.containers.tqe.configuration.TQEConfigurator;
import org.testcontainers.utility.DockerImageName;

public class TestClass {

  @Test
  public void test() {
    final Path publisherConfigPath = Paths.get("path/to/simple-grpc-publisher.yml");
    final Path consumerConfigPath = Paths.get("path/to/`simple-grpc-consumer.yml`");
    final Path queueConfigPath = Paths.get("path/to/simple.queue.yml");
    final DockerImageName image = DockerImageName.parse("tqe-image-name:tag");

    try (TQEConfigurator configurator =
        FileTQEConfigurator.builder(image, queueConfigPath,
            Set.of(publisherConfigPath, consumerConfigPath));
        TQECluster cluster = new TQEClusterImpl(configurator)
    ) {
      cluster.start();

      // получим grpc точки (отдельный для publisher и consumer)
      final Map<String, GrpcContainer<?>> grpc = cluster.grpc();
      Assertions.assertEquals(2, grpc.size());

      // получим узлы очереди
      final Map<String, TarantoolContainer<?>> queue = cluster.queue();
      Assertions.assertEquals(1, queue.size());

      // перезапустим кластер
      cluster.restart(1, TimeUnit.SECONDS, 1, TimeUnit.SECONDS);
    }
  }
}
```

### Пример 3. Манипуляции с отдельными узлами кластера

Для перезапуска или остановки отдельных узлов можно воспользоваться следующим кодом:

```java
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.tqe.GrpcContainer;
import org.testcontainers.containers.tqe.TQECluster;
import org.testcontainers.containers.tqe.TQEClusterImpl;
import org.testcontainers.containers.tqe.configuration.FileTQEConfigurator;
import org.testcontainers.containers.tqe.configuration.TQEConfigurator;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;

public class TestClass {

  @Test
  public void test() {
    final Path publisherConfigPath = Paths.get("path/to/simple-grpc-publisher.yml");
    final Path consumerConfigPath = Paths.get("path/to/`simple-grpc-consumer.yml`");
    final Path queueConfigPath = Paths.get("path/to/simple.queue.yml");
    final DockerImageName image = DockerImageName.parse("tqe-image-name:tag");

    try (TQEConfigurator configurator =
        FileTQEConfigurator.builder(image, queueConfigPath,
            Set.of(publisherConfigPath, consumerConfigPath));
        TQECluster cluster = new TQEClusterImpl(configurator)
    ) {
      cluster.start();

      // получим grpc точки (отдельный для publisher и consumer)
      final Map<String, GrpcContainer<?>> grpc = cluster.grpc();
      // Остановим все grpc-точки
      grpc.values().parallelStream().forEach(Startable::stop);

      // запустим заново
      grpc.values().parallelStream().forEach(Startable::start);

      // получим узлы очереди
      final Map<String, TarantoolContainer<?>> queue = cluster.queue();

      // Остановим узлы с сохранением состояния
      queue.values().parallelStream().forEach(TarantoolContainer::stopWithSafeMount);

      // Запустим заново
      queue.values().parallelStream().forEach(TarantoolContainer::start);
    }
  }
}
```

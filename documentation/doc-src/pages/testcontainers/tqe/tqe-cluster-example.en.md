---
title: Examples
---

## TQE Testcontainers Usage Examples

### Example 1. Starting a TQE Cluster with One Grpc Node

Let's define a configuration file for the grpc node (`simple-grpc.yml`):

```yaml
# metrics port (required)
core_port: 1111

# grpc server address (required)
grpc_listen:
  - uri: 'tcp://0.0.0.0:18182'

# Indicate that the node is a publisher (grpc-publisher)
publisher:
  enabled: true
  # connection settings to Tarantool nodes
  tarantool:
    user: test-super
    pass: test
    connections:
      # Router addresses
      routers:
        - "router:3301"

# Indicate that the node is also a consumer (grpc-consumer)
consumer:
  enabled: true
  tarantool:
    user: test-super
    pass: test
    connections:
      # List of storage node addresses for Tarantool
      storage:
        - "master:3301"
```

Let's define the configuration for Tarantool nodes (`simple-queue.yml`):

```yaml
# Credentials
credentials:
  users:
    # Required test user 
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

# Define a test queue
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
        # Required role          
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
          
        # Required role
        roles: [ app.roles.queue ]
        leader: master
        instances:
          master:
            iproto:
              listen:
                - uri: master:3301
```

To start a TQE cluster, we'll use the following code:

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

      // get grpc nodes
      final Map<String, GrpcContainer<?>> grpc = cluster.grpc();
      Assertions.assertEquals(1, grpc.size());

      // get queue nodes
      final Map<String, TarantoolContainer<?>> queue = cluster.queue();
      Assertions.assertEquals(1, queue.size());

      // restart the cluster
      cluster.restart(1, TimeUnit.SECONDS, 1, TimeUnit.SECONDS);
    }
  }
}
```

### Example 2. Cluster with Separate Nodes (Publisher/Publisher)

We'll use the queue configuration (Tarantool cluster) defined in `Example 1`. Let's define separate configurations for `grpc-consumer` and `grpc-publisher`.

Configuration for `grpc-publisher` (`simple-grpc-publisher.yml`):

```yaml
# metrics port (required)
core_port: 1111

# grpc server address (required)
grpc_listen:
  - uri: 'tcp://0.0.0.0:18182'

# Indicate that the node is a publisher (grpc-publisher)
publisher:
  enabled: true
  # connection settings to Tarantool nodes
  tarantool:
    user: test-super
    pass: test
    connections:
      # Router addresses
      routers:
        - "router:3301"
```

Configuration for `grpc-consumer` (`simple-grpc-consumer.yml`):

```yaml
# metrics port (required)
core_port: 1111

# grpc server address (required)
grpc_listen:
  - uri: 'tcp://0.0.0.0:18182'

# Indicate that the node is a consumer (grpc-consumer)
consumer:
  enabled: true
  tarantool:
    user: test-super
    pass: test
    connections:
      # List of storage node addresses for Tarantool
      storage:
        - "master:3301"
```

To start the cluster, we'll use the following code:

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

      // get grpc nodes (separate for publisher and consumer)
      final Map<String, GrpcContainer<?>> grpc = cluster.grpc();
      Assertions.assertEquals(2, grpc.size());

      // get queue nodes
      final Map<String, TarantoolContainer<?>> queue = cluster.queue();
      Assertions.assertEquals(1, queue.size());

      // restart the cluster
      cluster.restart(1, TimeUnit.SECONDS, 1, TimeUnit.SECONDS);
    }
  }
}
```

### Example 3. Manipulating Individual Cluster Nodes

To restart or stop individual nodes, you can use the following code:

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

      // get grpc nodes (separate for publisher and consumer)
      final Map<String, GrpcContainer<?>> grpc = cluster.grpc();
      // Stop all grpc nodes
      grpc.values().parallelStream().forEach(Startable::stop);

      // Start them again
      grpc.values().parallelStream().forEach(Startable::start);

      // get queue nodes
      final Map<String, TarantoolContainer<?>> queue = cluster.queue();

      // Stop nodes with state preservation
      queue.values().parallelStream().forEach(TarantoolContainer::stopWithSafeMount);

      // Start them again
      queue.values().parallelStream().forEach(TarantoolContainer::start);
    }
  }
}
```

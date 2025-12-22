/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.tqe.CommonTest;
import org.testcontainers.lifecycle.Startable;

class FileTQEConfiguratorTest extends CommonTest {

  @Test
  void simpleConfiguration() {
    try (TQEConfigurator configurator = FileTQEConfigurator.builder(IMAGE_NAME, SIMPLE_QUEUE_CONFIG,
        Set.of(SIMPLE_GRPC_CONFIG)).build()) {
      configurator.queue().values().parallelStream().forEach(Startable::start);
      configurator.grpc().values().parallelStream().forEach(Startable::start);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Stream<String> dataForTestInvalidQueueConfigShouldThrow() {
    return Stream.of(
        // router have no required roles
        """
            # Credentials
            credentials:
              users:
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
            # queues configs
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
                    leader: master
                    instances:
                      master:
                        iproto:
                          listen:
                            - uri: master:3301
            """,
        """
            # Credentials
            credentials:
              users:
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
            # queues configs
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
                    leader: master
                    instances:
                      master:
                        iproto:
                          net_msg_max: 768
            """

    );
  }

  @ParameterizedTest
  @MethodSource("dataForTestInvalidQueueConfigShouldThrow")
  void testInvalidQueueConfig(String invalidQueueConfig) throws IOException {
    final Path invalidConfigPath = TEST_TEMP_DIR.resolve(UUID.randomUUID().toString());
    Files.writeString(invalidConfigPath, invalidQueueConfig);

    Assertions.assertThrows(ContainerLaunchException.class, () -> {
      try (FileTQEConfigurator c = FileTQEConfigurator.builder(IMAGE_NAME, invalidConfigPath,
          Set.of(SIMPLE_GRPC_CONFIG)).build()) {
      }
    });
  }

  public static Stream<Arguments> dataForTestInvalidConfigsPaths() {
    return Stream.of(
        // invalid grpc configs
        // null
        Arguments.of(
            SIMPLE_QUEUE_CONFIG,
            null
        ),
        // empty
        Arguments.of(
            SIMPLE_QUEUE_CONFIG,
            Set.of()
        ),
        // non regular
        Arguments.of(
            SIMPLE_QUEUE_CONFIG,
            Set.of(TEST_TEMP_DIR)
        ),

        // invalid queue config
        Arguments.of(
            null,
            Set.of(SIMPLE_GRPC_CONFIG)
        ),
        Arguments.of(
            TEST_TEMP_DIR,
            Set.of(SIMPLE_GRPC_CONFIG)
        )
    );
  }

  @ParameterizedTest
  @MethodSource("dataForTestInvalidConfigsPaths")
  void testInvalidConfigsPaths(Path invalidGrpcConfig, Set<Path> invalidQueueConfigs) {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      try (FileTQEConfigurator c = FileTQEConfigurator.builder(IMAGE_NAME, invalidGrpcConfig, invalidQueueConfigs)
          .build()) {
      }
    });
  }
}

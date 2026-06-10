/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tqe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.tqe.TQECluster;
import org.testcontainers.containers.tqe.TQEClusterImpl;
import org.testcontainers.containers.tqe.configuration.TQEConfigurator;

class TQEClusterTest {

  @ParameterizedTest
  @EnumSource(TQEVersion.class)
  void testStartupAndShutdown(TQEVersion version) {
    try (TQEClusterFixture fx = new TQEClusterFixture(version)) {
      // Fixture starts the cluster in its constructor; close() stops it.
    }
  }

  @ParameterizedTest
  @EnumSource(TQEVersion.class)
  void testRestartMethod(TQEVersion version) throws Exception {
    try (TQEClusterFixture fx = new TQEClusterFixture(version)) {
      fx.restart(1, TimeUnit.SECONDS, 1, TimeUnit.SECONDS);
    }
  }

  public static Stream<Arguments> dataForTestInvalidQueueConfig() {
    final List<String> invalidConfigs =
        Arrays.asList(
            // no required test-super user
            """
            # Credentials
            credentials:
              users:
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
                          listen:
                            - uri: master:3301
                          net_msg_max: 768
            """,
            // no consumer storage to connect from grpc
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
            """);

    return TQEVersion.all()
        .flatMap(
            version ->
                invalidConfigs.stream()
                    .map(
                        s -> {
                          final Path testConfigPath =
                              TQETestHelper.TEST_TEMP_DIR.resolve(UUID.randomUUID().toString());
                          try {
                            Files.writeString(testConfigPath, s);
                            return Arguments.of(version, testConfigPath);
                          } catch (IOException e) {
                            throw new RuntimeException(e);
                          }
                        }));
  }

  @ParameterizedTest
  @MethodSource("dataForTestInvalidQueueConfig")
  void testInvalidQueueConfig(TQEVersion version, Path queueConfig) {
    Assertions.assertThrows(
        ContainerLaunchException.class,
        () -> {
          try (TQEConfigurator configurator =
                  version
                      .configuratorBuilder(queueConfig, Set.of(version.grpcConfig()))
                      .withStartupTimeout(Duration.ofSeconds(5))
                      .build();
              TQECluster cluster = new TQEClusterImpl(configurator)) {
            cluster.start();
          }
        });
  }

  public static Stream<Arguments> dataForTestInvalidGrpcConfig() {
    return TQEVersion.all()
        .flatMap(
            version -> {
              final List<String> invalidGrpcConfigs =
                  Arrays.asList(
                      // unknown host
                      """
                      core_port: 1111
                      grpc_listen:
                        - uri: 'tcp://0.0.0.0:18182'

                      %s:
                        enabled: true
                        tarantool:
                          user: test-super
                          pass: test
                          connections:
                            routers:
                              - "unknown:3301"

                      consumer:
                        enabled: true
                        tarantool:
                          user: test-super
                          pass: test
                          connections:
                            storage:
                              - "master:3301"
                      """
                          .formatted(version.producerRoleName()),
                      // no consumers and producers
                      """
                      core_port: 1111
                      grpc_listen:
                        - uri: 'tcp://0.0.0.0:18182'

                      %s:
                        enabled: false
                        tarantool:
                          user: test-super
                          pass: test
                          connections:
                            routers:
                              - "router:3301"

                      consumer:
                        enabled: false
                        tarantool:
                          user: test-super
                          pass: test
                          connections:
                            storage:
                              - "master:3301"
                      """
                          .formatted(version.producerRoleName()),
                      // no core_port parameter
                      """
                      grpc_listen:
                        - uri: 'tcp://0.0.0.0:18182'

                      %s:
                        enabled: true
                        tarantool:
                          user: test-super
                          pass: test
                          connections:
                            routers:
                              - "router:3301"

                      consumer:
                        enabled: true
                        tarantool:
                          user: test-super
                          pass: test
                          connections:
                            storage:
                              - "master:3301"
                      """
                          .formatted(version.producerRoleName()),
                      // no listen.uri parameter
                      """
                      core_port: 1111

                      %s:
                        enabled: true
                        tarantool:
                          user: test-super
                          pass: test
                          connections:
                            routers:
                              - "router:3301"

                      consumer:
                        enabled: true
                        tarantool:
                          user: test-super
                          pass: test
                          connections:
                            storage:
                              - "master:3301"
                      """
                          .formatted(version.producerRoleName()));

              return invalidGrpcConfigs.stream()
                  .map(
                      s -> {
                        final Path testConfigPath =
                            TQETestHelper.TEST_TEMP_DIR.resolve(UUID.randomUUID() + ".yml");
                        try {
                          Files.writeString(testConfigPath, s);
                          return Arguments.of(version, testConfigPath);
                        } catch (IOException e) {
                          throw new RuntimeException(e);
                        }
                      });
            });
  }

  @ParameterizedTest
  @MethodSource("dataForTestInvalidGrpcConfig")
  void testInvalidGrpcConfig(TQEVersion version, Path grpcConfig) {
    Assertions.assertThrows(
        ContainerLaunchException.class,
        () -> {
          try (TQEConfigurator configurator =
                  version
                      .configuratorBuilder(version.queueConfig(), Set.of(grpcConfig))
                      .withStartupTimeout(Duration.ofSeconds(5))
                      .build();
              TQECluster cluster = new TQEClusterImpl(configurator)) {
            cluster.start();
          }
        });
  }
}

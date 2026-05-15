/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tqe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.instancio.Select;
import org.instancio.generators.Generators;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.tqe.GrpcContainer;
import org.testcontainers.containers.tqe.GrpcContainer.GrpcRole;
import org.testcontainers.containers.tqe.TQECluster;
import org.testcontainers.containers.tqe.TQEClusterImpl;
import org.testcontainers.containers.tqe.configuration.FileTQEConfigurator;
import org.testcontainers.containers.tqe.configuration.TQEConfigurator;
import org.testcontainers.containers.utils.pojo.User;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import tarantool.queue_ee.Consumer.SubscriptionRequest;
import tarantool.queue_ee.Consumer.SubscriptionStreamRequest;
import tarantool.queue_ee.Consumer.SubscriptionStreamResponse;
import tarantool.queue_ee.ConsumerServiceGrpc;
import tarantool.queue_ee.ConsumerServiceGrpc.ConsumerServiceStub;
import tarantool.queue_ee.ProducerGrpc;
import tarantool.queue_ee.ProducerGrpc.ProducerBlockingStub;
import tarantool.queue_ee.ProducerOuterClass.ProduceMessage;
import tarantool.queue_ee.ProducerOuterClass.ProduceRequest;

class TQEClusterImplTest extends CommonTest {

  @RepeatedTest(10)
  void testMultiplyRestart() throws Exception {
    try (TQEConfigurator configurator =
            FileTQEConfigurator.builder(IMAGE_NAME, SIMPLE_QUEUE_CONFIG, Set.of(SIMPLE_GRPC_CONFIG))
                .build();
        TQECluster cluster = new TQEClusterImpl(configurator)) {
      cluster.start();
    }
  }

  @Test
  void testRestartMethod() throws Exception {
    try (TQEConfigurator configurator =
            FileTQEConfigurator.builder(IMAGE_NAME, SIMPLE_QUEUE_CONFIG, Set.of(SIMPLE_GRPC_CONFIG))
                .build();
        TQECluster cluster = new TQEClusterImpl(configurator)) {
      cluster.start();
      cluster.restart(1, TimeUnit.SECONDS, 1, TimeUnit.SECONDS);
    }
  }

  public static Stream<Arguments> dataForTestInvalidQueueConfigShouldThrow() {
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

    return invalidConfigs.stream()
        .map(
            s -> {
              final Path testConfigPath = TEST_TEMP_DIR.resolve(UUID.randomUUID().toString());
              try {
                Files.writeString(testConfigPath, s);
                return Arguments.of(testConfigPath);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  @ParameterizedTest
  @MethodSource("dataForTestInvalidQueueConfigShouldThrow")
  void testInvalidQueueConfigShouldThrow(Path queueConfig) {
    Assertions.assertThrows(
        ContainerLaunchException.class,
        () -> {
          try (TQEConfigurator configurator =
                  FileTQEConfigurator.builder(IMAGE_NAME, queueConfig, Set.of(SIMPLE_GRPC_CONFIG))
                      .withStartupTimeout(Duration.ofSeconds(5))
                      .build();
              TQECluster cluster = new TQEClusterImpl(configurator)) {
            cluster.start();
          }
        });
  }

  public static Stream<Path> dataForTestInvalidGrpcConfig() {
    final List<String> invalidGrpcConfigs =
        Arrays.asList(
            """
            core_port: 1111
            grpc_listen:
              - uri: 'tcp://0.0.0.0:18182'

            producer:
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
            """,
            // no consumers and producers
            """
            core_port: 1111
            grpc_listen:
              - uri: 'tcp://0.0.0.0:18182'

            producer:
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
            """,
            // no core_port parameter
            """
            grpc_listen:
              - uri: 'tcp://0.0.0.0:18182'

            producer:
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
            """,
            // no listen.uri parameter
            """
            core_port: 1111

            producer:
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
            """);

    return invalidGrpcConfigs.stream()
        .map(
            s -> {
              final Path testConfigPath = TEST_TEMP_DIR.resolve(UUID.randomUUID() + ".yml");
              try {
                Files.writeString(testConfigPath, s);
                return testConfigPath;
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  @ParameterizedTest
  @MethodSource("dataForTestInvalidGrpcConfig")
  void testInvalidGrpcConfig(Path grpcConfig) {
    Assertions.assertThrows(
        ContainerLaunchException.class,
        () -> {
          try (TQEConfigurator configurator =
                  FileTQEConfigurator.builder(IMAGE_NAME, SIMPLE_QUEUE_CONFIG, Set.of(grpcConfig))
                      .withStartupTimeout(Duration.ofSeconds(5))
                      .build();
              TQECluster cluster = new TQEClusterImpl(configurator)) {
            cluster.start();
          }
        });
  }

  @RepeatedTest(10)
  void testPublishAndConsumeData() {
    Assertions.assertDoesNotThrow(
        () -> {
          final ObjectMapper MAPPER = new ObjectMapper();

          try (TQEConfigurator configurator =
                  FileTQEConfigurator.builder(
                          IMAGE_NAME, SIMPLE_QUEUE_CONFIG, Set.of(SIMPLE_GRPC_CONFIG))
                      .build();
              TQECluster cluster = new TQEClusterImpl(configurator)) {
            cluster.start();

            final String queueName = "test";

            final List<GrpcContainer<?>> producers =
                cluster.grpc().values().stream()
                    .filter(g -> g.roles().contains(GrpcRole.PRODUCER))
                    .toList();
            final List<GrpcContainer<?>> consumers =
                cluster.grpc().values().stream()
                    .filter(g -> g.roles().contains(GrpcRole.CONSUMER))
                    .toList();

            Assertions.assertFalse(producers.isEmpty());
            Assertions.assertFalse(consumers.isEmpty());

            final Set<InetSocketAddress> grpcAddresses = producers.get(0).grpcAddresses();
            final Set<InetSocketAddress> consumerAddresses = consumers.get(0).grpcAddresses();

            final Optional<InetSocketAddress> publisherAddress = grpcAddresses.stream().findFirst();
            Assertions.assertTrue(publisherAddress.isPresent());
            final Optional<InetSocketAddress> consumerAddress =
                consumerAddresses.stream().findFirst();
            Assertions.assertTrue(consumerAddress.isPresent());

            final ManagedChannel producerChannel =
                ManagedChannelBuilder.forAddress(
                        publisherAddress.get().getHostName(), publisherAddress.get().getPort())
                    .usePlaintext()
                    .build();

            final ManagedChannel consumerChannel =
                ManagedChannelBuilder.forAddress(
                        consumerAddress.get().getHostName(), consumerAddress.get().getPort())
                    .usePlaintext()
                    .build();

            final ProducerBlockingStub producer =
                ProducerGrpc.newBlockingStub(producerChannel);
            final ConsumerServiceStub consumer = ConsumerServiceGrpc.newStub(consumerChannel);

            final List<User> users =
                Instancio.ofList(User.class)
                    .size(100)
                    .generate(
                        Select.field(User::getName),
                        g -> g.string().alphaNumeric().allowEmpty().nullable())
                    .generate(Select.field(User::getAge), Generators::ints)
                    .create();

            final ProduceRequest.Builder requestBuilder = ProduceRequest
                .newBuilder()
                .setQueue(queueName);
            for (User user : users) {
              requestBuilder.addMessages(
                  ProduceMessage.newBuilder()
                      .setPayload(ByteString.copyFrom(MAPPER.writeValueAsBytes(user))));
            }
            final ProduceRequest produceRequest = requestBuilder.build();
            producer.produce(produceRequest);

            final Set<User> result = new CopyOnWriteArraySet<>();
            StreamObserver<SubscriptionStreamRequest> requestsStream = consumer.subscribe(
                new StreamObserver<SubscriptionStreamResponse>() {
                  @Override
                  public void onNext(SubscriptionStreamResponse response) {
                    response.getNotifications().getNotificationsList().stream()
                        .map(
                            n -> {
                              try {
                                return MAPPER.readValue(
                                    n.getMessage().getPayload().toByteArray(), User.class);
                              } catch (IOException e) {
                                throw new RuntimeException(e);
                              }
                            })
                        .forEach(result::add);
                  }

                  @Override
                  public void onError(Throwable t) {}

                  @Override
                  public void onCompleted() {}
                }
            );
            requestsStream.onNext(
              SubscriptionStreamRequest
                  .newBuilder()
                  .setSubscribeRequest(
                      SubscriptionRequest
                          .newBuilder()
                          .setCursor("")
                          .setQueue(queueName)
                      )
                  .build()
            );

            Unreliables.retryUntilTrue(
                5, TimeUnit.SECONDS, () -> new LinkedHashSet<>(users).size() == result.size());
            Assertions.assertEquals(new LinkedHashSet<>(users), result);
            consumerChannel.shutdownNow();
            producerChannel.shutdownNow();
          }
        });
  }
}

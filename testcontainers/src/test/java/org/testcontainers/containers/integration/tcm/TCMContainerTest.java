/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tcm;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.launcher.Etcd;
import io.etcd.jetcd.launcher.EtcdContainer;
import io.etcd.jetcd.options.GetOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.tcm.TCMContainer;
import org.testcontainers.containers.tcm.config.TCMConfig;
import org.testcontainers.containers.utils.Utils;

class TCMContainerTest {

  private static final Path PATH_TO_TARANTOOL_CONFIG;

  private static final String ETCD_NAME = "etcd";

  private static final List<String> EMPTY_LIST = new ArrayList<>();

  private static final String TARANTOOL_DB_IMAGE_NAME =
      System.getenv().getOrDefault("TARANTOOL_REGISTRY", "") + "tarantooldb:2.2.1";

  static {
    try {
      PATH_TO_TARANTOOL_CONFIG =
          Paths.get(
              Objects.requireNonNull(
                      TCMContainerTest.class.getClassLoader().getResource("tdb/tarantool.yaml"))
                  .toURI());

    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static Stream<Arguments> dataForTCM() {
    return Stream.of(
        Arguments.of(
            new EtcdContainer(
                Utils.resolveContainerImage("testcontainers.etcd.image", Etcd.CONTAINER_IMAGE),
                ETCD_NAME,
                EMPTY_LIST),
            new TCMContainer(
                TARANTOOL_DB_IMAGE_NAME,
                "tcm",
                TCMConfig.builder()
                    .withEtcdAddress("http://" + ETCD_NAME + ":" + Etcd.ETCD_CLIENT_PORT)
                    .build(),
                PATH_TO_TARANTOOL_CONFIG)));
  }

  @ParameterizedTest
  @MethodSource("dataForTCM")
  void testTCMContainerEtcdAvailable(EtcdContainer etcd, TCMContainer tcm) {
    assertDoesNotThrow(
        () -> {
          try (Network net = Network.newNetwork()) {
            Duration startupDuration = Duration.ofSeconds(10);
            etcd.withStartupTimeout(startupDuration).withNetwork(net);

            tcm.withStartupTimeout(startupDuration).withNetwork(net);

            etcd.start();
            tcm.start();
          }
        });
  }

  @ParameterizedTest
  @MethodSource("dataForTCM")
  void testTCMContainerEtcdNotAvailableShouldThrow(EtcdContainer etcd, TCMContainer tcm) {
    assertThrows(
        ContainerLaunchException.class,
        () -> {
          try (Network net = Network.newNetwork()) {
            Duration startupDuration = Duration.ofSeconds(10);
            etcd.withStartupTimeout(startupDuration).withNetwork(net);

            tcm.withStartupTimeout(startupDuration);
            etcd.start();
            tcm.start();
          }
        });
  }

  @ParameterizedTest
  @MethodSource("dataForTCM")
  void testPublishConfigurationsWithAvailableEtcd(EtcdContainer etcd, TCMContainer tcm) {
    BiFunction<KV, String, Long> getKVCount =
        (client, prefix) ->
            client
                .get(
                    ByteSequence.from(prefix.getBytes(StandardCharsets.UTF_8)),
                    GetOption.builder().isPrefix(true).build())
                .join()
                .getCount();

    assertDoesNotThrow(
        () -> {
          try (Network net = Network.newNetwork()) {
            Duration startupDuration = Duration.ofSeconds(10);
            etcd.withStartupTimeout(startupDuration).withNetwork(net);

            tcm.withStartupTimeout(startupDuration).withNetwork(net);

            etcd.start();

            final String etcdEndpoint =
                "http://localhost:" + etcd.getMappedPort(Etcd.ETCD_CLIENT_PORT);

            try (Client commonEtcdClient = Client.builder().endpoints(etcdEndpoint).build()) {
              final KV client = commonEtcdClient.getKVClient();
              final long tcmKvCountBeforeTcmStart = getKVCount.apply(client, "/tcm");
              final long configKvCountBeforeTcmStart = getKVCount.apply(client, "/tdb");
              assertEquals(0, tcmKvCountBeforeTcmStart);
              assertEquals(0, configKvCountBeforeTcmStart);

              tcm.start();

              final long tcmKvCountAfterTcmStart = getKVCount.apply(client, "/tcm");
              final long configKvCountAfterTcmStart = getKVCount.apply(client, "/tdb");
              assertTrue(tcmKvCountAfterTcmStart > 0);
              assertEquals(0, configKvCountAfterTcmStart);

              tcm.publishConfig();

              final long configKvCountAfterTcmConfigPublish = getKVCount.apply(client, "/tdb");
              assertTrue(configKvCountAfterTcmConfigPublish > 0);
            }
          }
        });
  }

  @ParameterizedTest
  @MethodSource("dataForTCM")
  void testPublishConfigurationsWithUnavailableEtcdShouldThrow(
      EtcdContainer etcd, TCMContainer tcm) {
    assertThrows(
        ContainerLaunchException.class,
        () -> {
          try (Network net = Network.newNetwork()) {
            Duration startupDuration = Duration.ofSeconds(10);
            etcd.withStartupTimeout(startupDuration).withNetwork(net);

            tcm.withStartupTimeout(startupDuration).withNetwork(net);

            etcd.start();
            tcm.start();

            etcd.close();
            tcm.publishConfig();
          }
        });
  }

  @Test
  void testPublishConfigurationsWithInvalidEtcdAddressShouldThrow() {
    Duration startupDuration = Duration.ofSeconds(10);
    final String addressWithoutSchema = ETCD_NAME + ":" + Etcd.ETCD_CLIENT_PORT;
    assertThrows(
        ContainerLaunchException.class,
        () -> {
          try (final Network net = Network.newNetwork();
              final EtcdContainer etcd =
                  new EtcdContainer(
                          Utils.resolveContainerImage(
                              "testcontainers.etcd.image", Etcd.CONTAINER_IMAGE),
                          ETCD_NAME,
                          EMPTY_LIST)
                      .withStartupTimeout(startupDuration)
                      .withNetwork(net);
              final TCMContainer tcm =
                  new TCMContainer(
                          TARANTOOL_DB_IMAGE_NAME,
                          "tcm",
                          TCMConfig.builder().withEtcdAddress(addressWithoutSchema).build(),
                          PATH_TO_TARANTOOL_CONFIG)
                      .withStartupTimeout(startupDuration)
                      .withNetwork(net)) {
            etcd.start();
            tcm.start();
            tcm.publishConfig();
          }
        });
  }

  @Test
  void testPublishConfigurationsWithNullTCMOptionsShouldThrow() {
    assertThrows(
        NullPointerException.class,
        () -> {
          try (final TCMContainer tcm =
              new TCMContainer(TARANTOOL_DB_IMAGE_NAME, "tcm", null, PATH_TO_TARANTOOL_CONFIG)) {
            tcm.start();
            tcm.publishConfig();
          }
        });
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tdb;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.tdb.TDB2ClusterImpl;
import org.testcontainers.containers.tdb.TDBCluster;
import org.testcontainers.containers.utils.Utils;

import io.tarantool.autogen.Tarantool3Configuration;

class TDB2ClusterImplTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(TDB2ClusterImplTest.class);

  private static final Path PATH_TO_TARANTOOL_CONFIG;
  private static final Path PATH_TO_TARANTOOL_INVALID_CONFIG;
  private static final Path PATH_TO_TARANTOOL_CONFIG_WITHOUT_TEST_USER;
  private static final Path MIGRATIONS_PATH;
  private static final Path MIGRATIONS_PATH_WITH_DIRS;
  private static final Duration STARTUP_TIMEOUT;
  private static final String TARANTOOL_DB_IMAGE_NAME;
  private static final YAMLMapper MAPPER;

  static {
    try {
      MAPPER = new YAMLMapper();
      STARTUP_TIMEOUT = Duration.ofMinutes(1);
      TARANTOOL_DB_IMAGE_NAME =
          System.getenv().getOrDefault("TARANTOOL_REGISTRY", "") + "tarantooldb:2.2.1";
      PATH_TO_TARANTOOL_CONFIG =
          Paths.get(
              Objects.requireNonNull(
                      TDB2ClusterImplTest.class.getClassLoader().getResource("tdb/tarantool.yaml"))
                  .toURI());
      PATH_TO_TARANTOOL_INVALID_CONFIG =
          Paths.get(
              Objects.requireNonNull(
                      TDB2ClusterImplTest.class
                          .getClassLoader()
                          .getResource("tdb/invalid_config.yaml"))
                  .toURI());
      PATH_TO_TARANTOOL_CONFIG_WITHOUT_TEST_USER =
          Paths.get(
              Objects.requireNonNull(
                      TDB2ClusterImplTest.class
                          .getClassLoader()
                          .getResource("tdb/tarantool_config_without_test_user.yaml"))
                  .toURI());

      MIGRATIONS_PATH =
          Paths.get(
              Objects.requireNonNull(
                      TDB2ClusterImplTest.class.getClassLoader().getResource("tdb/migration_dir"))
                  .toURI());
      MIGRATIONS_PATH_WITH_DIRS =
          Paths.get(
              Objects.requireNonNull(TDB2ClusterImplTest.class.getClassLoader().getResource("tdb"))
                  .toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static Stream<Arguments> dataForClusterWithMigrations() {
    return Stream.of(
        Arguments.of(null, "---\n- true\n...\n\n"),
        Arguments.of(MIGRATIONS_PATH, "---\n- false\n...\n\n"));
  }

  @ParameterizedTest
  @MethodSource("dataForClusterWithMigrations")
  void testMigrationsPath(Path pathToMigrationsDir, String expectedMessage) {
    Assertions.assertDoesNotThrow(
        () -> {
          final Tarantool3Configuration configuration =
              MAPPER.readValue(PATH_TO_TARANTOOL_CONFIG.toFile(), Tarantool3Configuration.class);

          try (TDBCluster cluster =
              TDB2ClusterImpl.builder(TARANTOOL_DB_IMAGE_NAME)
                  .withMigrationsDirectory(pathToMigrationsDir)
                  .withTDB2Configuration(configuration)
                  .withStartupTimeout(STARTUP_TIMEOUT)
                  .build()) {
            cluster.start();
            cluster
                .storages()
                .forEach(
                    (s, container) -> {
                      final String command =
                          "echo \"return box.space.kv == nil\" | tt connect " + s + ":" + 3301;
                      try {
                        Assertions.assertEquals(
                            expectedMessage,
                            Utils.execExceptionally(
                                LOGGER,
                                container.getContainerInfo(),
                                "",
                                "/bin/sh",
                                "-c",
                                command));
                      } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                      }
                    });
          }
        });
  }

  public static Stream<Arguments> dataForTestSimpleCluster() {

    return Stream.of(
        Arguments.of(1, 1, 1),
        Arguments.of(1, 2, 3),
        Arguments.of(1, 3, 2),
        Arguments.of(2, 1, 3),
        Arguments.of(2, 3, 1));
  }

  @ParameterizedTest
  @MethodSource("dataForTestSimpleCluster")
  void testSimpleCluster(int routerCount, int shardCount, int replicaCount) {
    try (TDBCluster cluster =
        TDB2ClusterImpl.builder(TARANTOOL_DB_IMAGE_NAME)
            .withMigrationsDirectory(MIGRATIONS_PATH)
            .withRouterCount(routerCount)
            .withShardCount(shardCount)
            .withReplicaCount(replicaCount)
            .withStartupTimeout(STARTUP_TIMEOUT)
            .build()) {

      cluster.start();
      String tuple = "{1, box.NULL, 1}";
      String insertCommand =
          "echo \"return crud.insert('kv', " + tuple + ")\" | tt connect %s:%s@%s:3301 -x lua";
      String routerCountCommand =
          "echo \"return crud.count('kv')\" | tt connect %s:%s@%s:3301 -x lua";
      String truncateCommand =
          "echo \"return crud.truncate('kv')\" | tt connect %s:%s@%s:3301 -x lua";
      Map<String, TarantoolContainer<?>> routers = cluster.routers();

      for (Entry<String, TarantoolContainer<?>> e : routers.entrySet()) {
        Assertions.assertTrue(
            Utils.execExceptionally(
                    LOGGER,
                    e.getValue().getContainerInfo(),
                    "",
                    "/bin/sh",
                    "-c",
                    String.format(
                        routerCountCommand,
                        TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER,
                        TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER_PWD,
                        e.getKey()))
                .contains("0, nil;"));
      }

      for (Entry<String, TarantoolContainer<?>> e : routers.entrySet()) {
        Utils.execExceptionally(
            LOGGER,
            e.getValue().getContainerInfo(),
            "",
            "/bin/sh",
            "-c",
            String.format(
                insertCommand,
                TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER,
                TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER_PWD,
                e.getKey()));
        Assertions.assertTrue(
            Utils.execExceptionally(
                    LOGGER,
                    e.getValue().getContainerInfo(),
                    "",
                    "/bin/sh",
                    "-c",
                    String.format(
                        routerCountCommand,
                        TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER,
                        TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER_PWD,
                        e.getKey()))
                .contains("1, nil;"));

        Utils.execExceptionally(
            LOGGER,
            e.getValue().getContainerInfo(),
            "",
            "/bin/sh",
            "-c",
            String.format(
                truncateCommand,
                TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER,
                TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER_PWD,
                e.getKey()));

        Assertions.assertTrue(
            Utils.execExceptionally(
                    LOGGER,
                    e.getValue().getContainerInfo(),
                    "",
                    "/bin/sh",
                    "-c",
                    String.format(
                        routerCountCommand,
                        TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER,
                        TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER_PWD,
                        e.getKey()))
                .contains("0, nil;"));
      }

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testClusterWithMigrationDirectoryWithDirsShouldThrow() {
    Assertions.assertThrows(
        ContainerLaunchException.class,
        () -> {
          final Tarantool3Configuration configuration =
              MAPPER.readValue(PATH_TO_TARANTOOL_CONFIG.toFile(), Tarantool3Configuration.class);

          try (TDBCluster cluster =
              TDB2ClusterImpl.builder(TARANTOOL_DB_IMAGE_NAME)
                  .withMigrationsDirectory(MIGRATIONS_PATH_WITH_DIRS)
                  .withTDB2Configuration(configuration)
                  .withStartupTimeout(STARTUP_TIMEOUT)
                  .build()) {
            cluster.start();
          }
        });
  }

  @Test
  void testClusterWithInvalidConfigShouldThrow() {
    Assertions.assertThrows(
        UnrecognizedPropertyException.class,
        () ->
            MAPPER.readValue(
                PATH_TO_TARANTOOL_INVALID_CONFIG.toFile(), Tarantool3Configuration.class));
  }

  @Test
  void testClusterWithConfigWithoutTestUserShouldThrow() throws IOException {
    Tarantool3Configuration configuration =
        MAPPER.readValue(
            PATH_TO_TARANTOOL_CONFIG_WITHOUT_TEST_USER.toFile(), Tarantool3Configuration.class);

    Assertions.assertThrows(
        ContainerLaunchException.class,
        () -> {
          try (TDBCluster cluster =
              TDB2ClusterImpl.builder(TARANTOOL_DB_IMAGE_NAME)
                  .withStartupTimeout(Duration.ofSeconds(30))
                  .withTDB2Configuration(configuration)
                  .build()) {
            cluster.start();
          }
        });
  }

  @Test
  void testFixedPorts() {
    Assertions.assertDoesNotThrow(
        () -> {
          try (TDBCluster cluster = TDB2ClusterImpl.builder(TARANTOOL_DB_IMAGE_NAME).build()) {
            cluster.start();

            final Map<String, TarantoolContainer<?>> routers = cluster.routers();
            routers
                .values()
                .forEach(
                    container -> {
                      Integer mappedPortBeforeStop =
                          container.getMappedPort(TDB2ClusterImpl.DEFAULT_IPROTO_TARANTOOL_PORT);
                      container.stopWithSafeMount();
                      container.start();
                      Integer mappedPortAfterStop =
                          container.getMappedPort(TDB2ClusterImpl.DEFAULT_IPROTO_TARANTOOL_PORT);
                      Assertions.assertEquals(mappedPortBeforeStop, mappedPortAfterStop);
                    });
          }
        });
  }

  @Test
  void testFixedUUID() {
    Assertions.assertDoesNotThrow(
        () -> {
          try (TDBCluster cluster =
              TDB2ClusterImpl.builder(TARANTOOL_DB_IMAGE_NAME).withReplicaCount(2).build()) {
            cluster.start();

            final Entry<String, TarantoolContainer<?>> router =
                cluster.routers().entrySet().stream().findFirst().get();

            final String command =
                "echo \"return box.info.uuid\" | tt connect " + router.getKey() + ":" + 3301;
            final ExecResult execResultBeforeRestart =
                router.getValue().execInContainer("/bin/sh", "-c", command);

            Assertions.assertEquals(0, execResultBeforeRestart.getExitCode());
            router.getValue().stopWithSafeMount();
            router.getValue().start();

            final ExecResult execResultAfterRestart =
                router.getValue().execInContainer("/bin/sh", "-c", command);
            Assertions.assertEquals(0, execResultAfterRestart.getExitCode());

            Assertions.assertEquals(execResultAfterRestart, execResultBeforeRestart);
          }
        });
  }

  @Test
  void testMountData() {
    Assertions.assertDoesNotThrow(
        () -> {
          try (TDBCluster cluster =
              TDB2ClusterImpl.builder(TARANTOOL_DB_IMAGE_NAME)
                  .withMigrationsDirectory(MIGRATIONS_PATH)
                  .withShardCount(2)
                  .withReplicaCount(2)
                  .withStartupTimeout(STARTUP_TIMEOUT)
                  .build()) {
            cluster.start();

            final BiConsumer<String, TarantoolContainer<?>> checkMigrations =
                (s, c) -> {
                  final String command =
                      "echo \"return box.space.kv == nil\" | tt connect " + s + ":" + 3301;
                  try {
                    Assertions.assertEquals(
                        "---\n- false\n...\n\n",
                        Utils.execExceptionally(
                            LOGGER, c.getContainerInfo(), "", "/bin/sh", "-c", command));
                  } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                };

            cluster.storages().forEach(checkMigrations);
            final List<CompletableFuture<?>> futures = new ArrayList<>(4);
            cluster
                .storages()
                .forEach((s, c) -> futures.add(CompletableFuture.runAsync(c::stopWithSafeMount)));
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            futures.clear();

            cluster.storages().forEach((s, c) -> futures.add(CompletableFuture.runAsync(c::start)));
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            futures.clear();

            cluster.storages().forEach(checkMigrations);
          }
        });
  }

  @RepeatedTest(5)
  void testSimpleMultipleRestart() throws InterruptedException {
    try (TDBCluster c = TDB2ClusterImpl.builder(TARANTOOL_DB_IMAGE_NAME).build()) {
      c.start();
      c.restart(1, TimeUnit.SECONDS);
    }
  }
}

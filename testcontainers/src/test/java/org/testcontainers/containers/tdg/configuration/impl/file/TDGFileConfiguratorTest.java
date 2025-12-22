/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdg.configuration.impl.file;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.tdg.TDGContainer;
import org.testcontainers.containers.tdg.Utils;
import org.testcontainers.containers.tdg.configuration.TDGConfigurator;
import org.testcontainers.containers.utils.pojo.User;
import org.testcontainers.utility.DockerImageName;

class TDGFileConfiguratorTest {

  private static final DockerImageName TDG_IMAGE = DockerImageName.parse(System.getenv().getOrDefault(
      "TARANTOOL_REGISTRY", "") + "tdg2:2.11.5-0-geff8adb3");

  final Path ROOT_PATH;
  final Path ROOT_WITHOUT_MIGRATIONS_CONFIG_PATH;
  final Path ROOT_WITH_MIGRATIONS_CONFIG_PATH;

  {
    try {
      ROOT_PATH = Paths.get(
          Objects.requireNonNull(TDGFileConfiguratorTest.class.getClassLoader().getResource("tdg/test-one-node"))
              .toURI());
      ROOT_WITHOUT_MIGRATIONS_CONFIG_PATH = ROOT_PATH.resolve("without-migrations");
      ROOT_WITH_MIGRATIONS_CONFIG_PATH = ROOT_PATH.resolve("with-migrations");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testConfigureIdempotent() throws Exception {
    try (TDGConfigurator c = new TDGFileConfigurator(ROOT_WITH_MIGRATIONS_CONFIG_PATH, TDG_IMAGE, Utils.uuid())) {
      final TDGContainer<?> core = c.core().getValue();
      core.start();

      Assertions.assertFalse(c.configured());
      c.configure();
      Assertions.assertTrue(c.configured());
      c.configure();
      Assertions.assertTrue(c.configured());
    }
  }

  @Test
  void testConfiguratorForOneNodeClusterWithConfig() throws Exception {
    try (TDGConfigurator c = new TDGFileConfigurator(ROOT_WITH_MIGRATIONS_CONFIG_PATH, TDG_IMAGE, Utils.uuid())) {
      Assertions.assertEquals(1, c.nodes().size());
      Assertions.assertNotNull(c.core());

      Assertions.assertFalse(c.configured());
      final TDGContainer<?> core = c.core().getValue();
      core.start();

      c.configure();
      Assertions.assertTrue(c.configured());
    }
  }

  @Test
  void testConfiguratorForOneNodeClusterWithoutConfig() throws Exception {
    try (TDGConfigurator c = new TDGFileConfigurator(ROOT_WITHOUT_MIGRATIONS_CONFIG_PATH, TDG_IMAGE, Utils.uuid())) {
      final Map<String, TDGContainer<?>> nodes = c.nodes();
      final Map.Entry<String, TDGContainer<?>> coreEntry = c.core();

      Assertions.assertEquals(1, nodes.size());

      final TDGContainer<?> core = coreEntry.getValue();
      core.start();

      Assertions.assertFalse(c.configured());
      c.configure();
      Assertions.assertTrue(c.configured());
    }
  }

  @Test
  void testConfigureOneNodeAndSendUser() throws Exception {
    try (TDGConfigurator c = new TDGFileConfigurator(ROOT_WITH_MIGRATIONS_CONFIG_PATH, TDG_IMAGE, Utils.uuid())) {
      final TDGContainer<?> core = c.core().getValue();
      core.start();

      c.configure();

      final List<User> users = Collections.singletonList(new User(50, "Petya"));
      final List<User> sentUsers = Utils.sendUsers(users, core);
      Assertions.assertEquals(users, sentUsers);
    }
  }

  @Test
  void testSaveDataMount() throws Exception {
    try (TDGConfigurator c = new TDGFileConfigurator(ROOT_WITH_MIGRATIONS_CONFIG_PATH, TDG_IMAGE, Utils.uuid())) {
      final TDGContainer<?> node = c.core().getValue();
      node.start();

      c.configure();

      final List<User> users = IntStream.range(1, 101).mapToObj(i -> new User(i, String.valueOf(i)))
          .collect(Collectors.toList());
      Assertions.assertEquals(users, Utils.sendUsers(users, node));

      node.stopWithSafeMount();
      node.start();

      final List<User> gotUsers = Utils.getUsers(users.size(), node);
      Assertions.assertEquals(users, gotUsers);
    }
  }
}

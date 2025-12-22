/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdg.cluster;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.tdg.TDGContainer;
import org.testcontainers.containers.tdg.Utils;
import org.testcontainers.containers.tdg.configuration.TDGConfigurator;
import org.testcontainers.containers.tdg.configuration.impl.file.TDGFileConfigurator;
import org.testcontainers.containers.utils.pojo.User;
import org.testcontainers.utility.DockerImageName;

class TDGClusterImplTest {

  private static final DockerImageName TDG_IMAGE = DockerImageName.parse(System.getenv().getOrDefault(
      "TARANTOOL_REGISTRY", "") + "tdg2:2.11.5-0-geff8adb3");

  private static final Path ROOT_CONFIG_PATH;

  static {
    try {
      ROOT_CONFIG_PATH = Paths.get(
          Objects.requireNonNull(TDGClusterImplTest.class.getClassLoader().getResource(
              "tdg/test-cluster-configuration")).toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testSimpleStart() throws Exception {
    try (TDGConfigurator c = new TDGFileConfigurator(ROOT_CONFIG_PATH, TDG_IMAGE, Utils.uuid());
        TDGCluster cluster = new TDGClusterImpl(c)) {
      cluster.start();
    }
  }

  @RepeatedTest(3)
  void testMultipleRestartCluster() throws Exception {
    try (TDGConfigurator c = new TDGFileConfigurator(ROOT_CONFIG_PATH, TDG_IMAGE, Utils.uuid());
        TDGCluster cluster = new TDGClusterImpl(c)) {
      cluster.start();
      cluster.restart(-1, TimeUnit.SECONDS, 1, TimeUnit.SECONDS);
    }
  }

  @Test
  void testSaveMountData() throws Exception {
    try (TDGConfigurator c = new TDGFileConfigurator(ROOT_CONFIG_PATH, TDG_IMAGE, Utils.uuid());
        TDGCluster cluster = new TDGClusterImpl(c)) {
      cluster.start();

      final TDGContainer<?> core = c.core().getValue();

      final List<User> users = IntStream.range(1, 100).mapToObj(i -> new User(i, String.valueOf(i)))
          .collect(Collectors.toList());
      final List<User> sentUsers = Utils.sendUsers(users, core);
      Assertions.assertEquals(users, sentUsers);

      cluster.restart(-1, TimeUnit.SECONDS, 1, TimeUnit.SECONDS);

      final List<User> gotUsers = Utils.getUsers(users.size(), core);
      Assertions.assertEquals(users, gotUsers);
    }
  }
}

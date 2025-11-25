/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.integration.tdg;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.tdg.cluster.TDGCluster;
import org.testcontainers.containers.tdg.cluster.TDGClusterImpl;
import org.testcontainers.containers.tdg.configuration.TDGConfigurator;
import org.testcontainers.containers.tdg.configuration.impl.file.TDGFileConfigurator;
import org.testcontainers.utility.DockerImageName;

import io.tarantool.client.OnlyKeyValueOptions;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.client.integration.PersonAsMap;
import io.tarantool.client.tdg.TarantoolDataGridClient;
import io.tarantool.client.tdg.TarantoolDataGridSpace;
import io.tarantool.mapping.slash.errors.TarantoolSlashErrorsException;

class TDGClientTest {

  private static final DockerImageName TDG_IMAGE =
      DockerImageName.parse(
          System.getenv().getOrDefault("TARANTOOL_REGISTRY", "") + "tdg2:2.11.5-0-geff8adb3");

  private static final Path ROOT_CONFIG_PATH;

  private static final TDGConfigurator configurator;

  private static final TDGCluster cluster;
  private static TarantoolDataGridClient client;

  static {
    try {
      ROOT_CONFIG_PATH =
          Paths.get(
              Objects.requireNonNull(
                      TDGClientTest.class
                          .getClassLoader()
                          .getResource("tdg/test-cluster-configuration"))
                  .toURI());
      configurator =
          new TDGFileConfigurator(ROOT_CONFIG_PATH, TDG_IMAGE, UUID.randomUUID().toString());
      cluster = new TDGClusterImpl(configurator);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeAll
  static void setUp() throws Exception {
    cluster.start();
    client =
        TarantoolFactory.tdg()
            .withHost(configurator.core().getValue().iprotoMappedAddress().getHostName())
            .withPort(configurator.core().getValue().iprotoMappedAddress().getPort())
            .build();
  }

  @AfterAll
  static void cleanUp() throws Exception {
    client.close();
    cluster.stop();
    configurator.close();
  }

  @AfterEach
  void after() {
    client.space("User").delete(Collections.singletonList(Arrays.asList("age", "==", 1))).join();
    client.space("person").delete(Collections.singletonList(Arrays.asList("id", "==", 1))).join();
  }

  @Test
  void testSimpleOperations() {
    TarantoolDataGridSpace space = client.space("User");
    HashMap<String, Object> tuple =
        new HashMap<String, Object>() {
          {
            put("age", 1);
            put("name", "a");
          }
        };
    Map<?, ?> putResult = space.put(tuple).join();
    Assertions.assertEquals(tuple, putResult);

    Assertions.assertEquals(tuple, space.get(1).join());

    List<Map<?, ?>> updateResult =
        space
            .update(
                Collections.singletonList(Arrays.asList("age", "==", 1)),
                Collections.singletonList(Arrays.asList("set", "name", "artyom")))
            .join();

    Assertions.assertEquals(1, updateResult.size());
    Assertions.assertEquals("artyom", updateResult.get(0).get("name"));

    List<Map<?, ?>> deleteResult =
        space.delete(Collections.singletonList(Arrays.asList("age", "==", 1))).join();
    Assertions.assertEquals(1, deleteResult.size());
    Assertions.assertEquals("artyom", deleteResult.get(0).get("name"));

    Assertions.assertNull(space.get(1).join());
  }

  @Test
  void testOptions() {
    TarantoolDataGridSpace space = client.space("User");
    HashMap<String, Object> tuple =
        new HashMap<String, Object>() {
          {
            put("age", 1);
            put("name", "a");
          }
        };
    Map<?, ?> putResult =
        space
            .put(
                tuple,
                OnlyKeyValueOptions.builder().withOption("skip_result", true).build(),
                Collections.emptyMap())
            .join();
    Assertions.assertNull(putResult);

    Assertions.assertEquals(tuple, space.get(1).join());

    List<Map<?, ?>> deleteResult =
        space.delete(Collections.singletonList(Arrays.asList("age", "==", 1))).join();
    Assertions.assertEquals(1, deleteResult.size());

    Assertions.assertNull(space.get(1).join());
  }

  @Test
  void testSimpleOperationsWithPojoAsInput() {
    TarantoolDataGridSpace space = client.space("person");
    HashMap<String, Object> tuple =
        new HashMap<String, Object>() {
          {
            put("id", 1);
            put("is_married", true);
            put("name", "artyom");
          }
        };
    Map<?, ?> putResult =
        space
            .put(
                PersonAsMap.builder()
                    .id((Integer) tuple.get("id"))
                    .isMarried((Boolean) tuple.get("is_married"))
                    .name((String) tuple.get("name"))
                    .build())
            .join();

    Assertions.assertEquals(tuple, putResult);
  }

  @Test
  void testFindAndCountOperations() {
    TarantoolDataGridSpace space = client.space("person");
    space.put(PersonAsMap.builder().id(1).isMarried(true).name("artyom").build()).join();
    space.put(PersonAsMap.builder().id(2).isMarried(false).name("kolya").build()).join();
    space.put(PersonAsMap.builder().id(3).isMarried(true).name("dima").build()).join();

    Assertions.assertEquals(
        2, space.count(Collections.singletonList(Arrays.asList("is_married", "==", true))).join());

    Assertions.assertEquals(
        Arrays.asList(
            new HashMap<String, Object>() {
              {
                put("cursor", "gaRzY2FukQE");
                put("id", 1);
                put("name", "artyom");
                put("is_married", true);
              }
            },
            new HashMap<String, Object>() {
              {
                put("cursor", "gaRzY2FukQM");
                put("id", 3);
                put("name", "dima");
                put("is_married", true);
              }
            }),
        space.find(Collections.singletonList(Arrays.asList("is_married", "==", true))).join());
  }

  @Test
  void testTDGThrowError() {
    TarantoolDataGridSpace space = client.space("NoSpace");
    CompletionException ex =
        Assertions.assertThrows(
            CompletionException.class, () -> space.put(Collections.emptyMap()).join());
    Throwable cause = ex.getCause();
    Assertions.assertEquals(TarantoolSlashErrorsException.class, cause.getClass());
    TarantoolSlashErrorsException tarantoolSlashErrorsException =
        (TarantoolSlashErrorsException) cause;
    Assertions.assertEquals(
        "Type \"NoSpace\" not found", tarantoolSlashErrorsException.getReason().getErr());
  }
}

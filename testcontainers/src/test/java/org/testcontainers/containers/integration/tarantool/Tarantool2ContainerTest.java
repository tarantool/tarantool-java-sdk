/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tarantool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.tarantool.Tarantool2Container;
import org.testcontainers.containers.tarantool.Tarantool2WaitStrategy;
import org.testcontainers.utility.DockerImageName;

// These test methods can be safely run in parallel.
@DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
@Execution(ExecutionMode.CONCURRENT)
public class Tarantool2ContainerTest {

  private static final DockerImageName IMAGE =
      DockerImageName.parse(
          System.getenv().getOrDefault("TARANTOOL_REGISTRY", "")
              + "tarantool/tarantool:2.11.2-ubuntu20.04");

  private static final Network NETWORK_FOR_TEST_CLASS = Network.newNetwork();

  private static final String USERNAME = "user";

  private static final String PASSWORD = "user";

  private static final String CREATE_USER_SCRIPT =
      String.format(
          "box.schema.user.create('%s', {password = '%s', if_not_exists = true})",
          USERNAME, PASSWORD);

  private static final String GRANT_USER_SCRIPT =
      String.format("box.schema.user.grant('%s', 'super')", USERNAME);

  private static final String BOX_ONCE_PATTERN = "box.once('init', function() %s end)";

  private static final String ECHO_FUNC = "echo = function(in_arg) return in_arg end";

  private static final String SPACE_PATTERN =
      "local s = box.schema.space.create('%s') s:format({{name = 'id', type = 'unsigned'},{name ="
          + " 'value', type = 'string'},}) s:create_index('primary', {parts = {'id'}})";

  @TempDir private static Path TEMP_DIR;

  @AfterAll
  static void afterAll() {
    NETWORK_FOR_TEST_CLASS.close();
  }

  public static Stream<Tarantool2Container.Builder> dataForTestWithoutConfig() throws IOException {
    final String totalScript =
        "box.cfg{}"
            + String.format(
                BOX_ONCE_PATTERN, CREATE_USER_SCRIPT + " " + GRANT_USER_SCRIPT + " " + ECHO_FUNC);

    final Path configPath =
        Files.write(
            TEMP_DIR.resolve(UUID.randomUUID().toString()),
            totalScript.getBytes(StandardCharsets.UTF_8));

    return Stream.of(
        Tarantool2Container.builder(IMAGE, configPath),
        Tarantool2Container.builder(IMAGE, totalScript));
  }

  @ParameterizedTest
  @MethodSource("dataForTestWithoutConfig")
  void testWithoutConfig(Tarantool2Container.Builder clientBuilder)
      throws IOException, InterruptedException {

    try (Tarantool2Container c = clientBuilder.build()) {
      c.withNetwork(NETWORK_FOR_TEST_CLASS)
          .waitingFor(new Tarantool2WaitStrategy(c.node(), USERNAME, PASSWORD));
      c.start();

      final String echoCommand = "echo \"return echo('%s')\" | tarantoolctl connect %s:%s@%s:3301";

      final String hello = "hello";
      final ExecResult execResult =
          c.execInContainer(
              "/bin/sh", "-c", String.format(echoCommand, hello, USERNAME, PASSWORD, c.node()));
      Assertions.assertEquals(0, execResult.getExitCode());
      Assertions.assertEquals(String.format("---\n- %s\n...\n\n", hello), execResult.getStdout());
    }
  }

  @Test
  void testWithParameter() throws IOException, InterruptedException {
    final String parameter = "worker_pool_threads";
    final int parameterVal = 5;

    final String CFG_CONFIG = String.format("box.cfg{%s=%s}", parameter, parameterVal);

    final String totalScript =
        CFG_CONFIG
            + " "
            + String.format(
                BOX_ONCE_PATTERN, CREATE_USER_SCRIPT + " " + GRANT_USER_SCRIPT + " " + ECHO_FUNC);

    try (Tarantool2Container c = Tarantool2Container.builder(IMAGE, totalScript).build()) {
      c.withNetwork(NETWORK_FOR_TEST_CLASS)
          .waitingFor(new Tarantool2WaitStrategy(c.node(), USERNAME, PASSWORD));
      c.start();

      final String echoCommand = "echo \"return box.cfg.%s\" | tarantoolctl connect %s:%s@%s:3301";

      final ExecResult execResult =
          c.execInContainer(
              "/bin/sh", "-c", String.format(echoCommand, parameter, USERNAME, PASSWORD, c.node()));
      Assertions.assertEquals(0, execResult.getExitCode());
      Assertions.assertEquals(
          String.format("---\n- %s\n...\n\n", parameterVal), execResult.getStdout());
    }
  }

  @Test
  void testFewStarts() {
    final String totalScript =
        "box.cfg{} "
            + String.format(
                BOX_ONCE_PATTERN, CREATE_USER_SCRIPT + " " + GRANT_USER_SCRIPT + " " + ECHO_FUNC);

    try (Tarantool2Container c = Tarantool2Container.builder(IMAGE, totalScript).build()) {
      c.withNetwork(NETWORK_FOR_TEST_CLASS)
          .waitingFor(new Tarantool2WaitStrategy(c.node(), USERNAME, PASSWORD));
      c.start();
      c.start();
    }
  }

  @Test
  void testStopWithSafeMount() throws IOException, InterruptedException {
    final String spaceName = "space";
    final String CREATE_SPACE_SCRIPT = String.format(SPACE_PATTERN, spaceName);

    final String totalScript =
        "box.cfg{} "
            + String.format(
                BOX_ONCE_PATTERN,
                CREATE_USER_SCRIPT + " " + GRANT_USER_SCRIPT + " " + CREATE_SPACE_SCRIPT);

    try (Tarantool2Container c = Tarantool2Container.builder(IMAGE, totalScript).build()) {
      c.withNetwork(NETWORK_FOR_TEST_CLASS)
          .waitingFor(new Tarantool2WaitStrategy(c.node(), USERNAME, PASSWORD));
      c.start();

      final String insertCommand =
          "echo \"box.space.%s:insert{%s,'%s'}\" | tarantoolctl connect %s:%s@%s:3301";

      final int id = 5;
      final String value = "val";

      final ExecResult insertExecResult =
          c.execInContainer(
              "/bin/sh",
              "-c",
              String.format(insertCommand, spaceName, id, value, USERNAME, PASSWORD, c.node()));
      Assertions.assertEquals(0, insertExecResult.getExitCode());
      Assertions.assertEquals(
          String.format("---\n- [%s, '%s']\n...\n\n", id, value), insertExecResult.getStdout());

      c.stopWithSafeMount();
      c.start();

      final String lenCommand =
          "echo \"return box.space.%s:len()\" | tarantoolctl connect %s:%s@%s:3301";
      final ExecResult lenExecResult =
          c.execInContainer(
              "/bin/sh", "-c", String.format(lenCommand, spaceName, USERNAME, PASSWORD, c.node()));
      Assertions.assertEquals(0, lenExecResult.getExitCode());
      Assertions.assertEquals("---\n- 1\n...\n\n", lenExecResult.getStdout());
    }
  }

  @Test
  void testClose() {
    final Tarantool2Container c = Tarantool2Container.builder(IMAGE, "box.cfg{}").build();
    c.start();
    c.close();

    Assertions.assertThrows(ContainerLaunchException.class, c::start);
  }

  public static Stream<String> dataForTestWrongInitialScript() {
    return Stream.of(
        // null initial script
        null,

        // empty
        "",

        // blank
        "       ",
        "box.cfg{",
        "box.cfg{unknown=4}",

        // setup port
        "box.cfg{listen=1234}",

        // setup host
        "box.cfg{listen='localhost:3301'}",

        // without box.cfg{}
        CREATE_USER_SCRIPT + " " + GRANT_USER_SCRIPT,

        // before box.cfg{}
        CREATE_USER_SCRIPT + " " + GRANT_USER_SCRIPT + " box.cfg{}");
  }

  @ParameterizedTest
  @MethodSource("dataForTestWrongInitialScript")
  void testWrongInitialScript(String initialScript) {
    final Duration startupTimeout = Duration.ofSeconds(3);

    Assertions.assertThrows(
        ContainerLaunchException.class,
        () -> {
          try (Tarantool2Container c = Tarantool2Container.builder(IMAGE, initialScript).build()) {
            c.setWaitStrategy(new Tarantool2WaitStrategy(c.node(), USERNAME, PASSWORD));
            c.withStartupTimeout(startupTimeout);
            c.start();
          }
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "     "})
  void testInvalidNodeName(String nodeName) {
    Assertions.assertThrows(
        ContainerLaunchException.class,
        () -> {
          try (Tarantool2Container c =
              Tarantool2Container.builder(IMAGE, "box.cfg{}").withNode(nodeName).build()) {
            c.start();
          }
        });
  }
}

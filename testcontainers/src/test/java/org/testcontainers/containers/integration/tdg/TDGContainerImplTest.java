/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tdg;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.tdg.TDGContainer;
import org.testcontainers.containers.tdg.TDGContainerImpl;
import org.testcontainers.utility.DockerImageName;

class TDGContainerImplTest {

  private static final DockerImageName TDG_IMAGE =
      DockerImageName.parse(
          System.getenv().getOrDefault("TARANTOOL_REGISTRY", "registry.ps.tarantool.io/")
              + "tdg2:2.11.5-0-geff8adb3");

  @Test
  @DisplayName("Проверьте, что контейнер запускается")
  void simpleTDGContainerStartTest() {
    try (TDGContainer<TDGContainerImpl> c = TDGContainerImpl.builder(TDG_IMAGE).build()) {
      c.start();
      Assertions.assertTrue(c.isRunning());
    }
  }

  @Test
  @DisplayName("Проверьте, что операции stop, start идемпотентны")
  void testSimpleStartStopIdempotency() {
    try (TDGContainer<TDGContainerImpl> c = TDGContainerImpl.builder(TDG_IMAGE).build()) {
      c.start();
      c.start();

      c.stopWithSafeMount();
      c.stopWithSafeMount();

      c.stop();
      c.stop();
    }
  }

  @Test
  @DisplayName(
      "Проверьте, что переменные окружения передаются в контейнер между созданием экземпляра и до"
          + " его запуска")
  void testToSetEnvs() throws IOException, InterruptedException {
    final Map<String, String> env =
        new HashMap<String, String>() {
          {
            put("TEST_ENV", "TEST_VALUE");
            put("TEST_ENV2", "TEST_VALUE2");
          }
        };

    try (TDGContainer<TDGContainerImpl> c = TDGContainerImpl.builder(TDG_IMAGE).build()) {
      c.withEnv(env);
      c.start();

      final ExecResult execResult = c.execInContainer("/bin/sh", "-c", "env");
      Assertions.assertEquals(0, execResult.getExitCode());

      final String output = execResult.getStdout();
      env.entrySet().stream()
          .map(e -> e.getKey() + "=" + e.getValue())
          .forEach(
              s -> {
                Assertions.assertTrue(output.contains(s));
              });
    }
  }

  public static Stream<String> dataForTestPortsAreOpen() {
    return Stream.of("tdg:3321", "test-tdg:9999", "test-test:3333");
  }

  @ParameterizedTest
  @MethodSource("dataForTestPortsAreOpen")
  @DisplayName("Проверьте, что контейнер доступен при передаче пользовательского advertiseUri")
  void testPortsAreOpen(String advertiseUri) throws IOException {
    try (TDGContainer<TDGContainerImpl> c =
        TDGContainerImpl.builder(TDG_IMAGE).withAdvertiseUri(advertiseUri).build()) {
      c.start();

      final Duration timeout = Duration.ofSeconds(30);
      final InetSocketAddress iprotoAddress = c.iprotoMappedAddress();
      final InetSocketAddress httpdAddress = c.httpMappedAddress();

      final List<InetSocketAddress> addresses = Arrays.asList(iprotoAddress, httpdAddress);
      for (InetSocketAddress address : addresses) {
        try (Socket socket = new Socket()) {
          socket.connect(address, (int) TimeUnit.SECONDS.toMillis(timeout.getSeconds()));
        }
      }
    }
  }

  public static Stream<Arguments> dataForTestFixedPorts() {
    return Stream.of(
        Arguments.of("test:4567", 4567),
        Arguments.of("test:1234", 1234),
        Arguments.of("test:1111", 1111));
  }

  @ParameterizedTest
  @MethodSource("dataForTestFixedPorts")
  void testFixedPorts(String advertiseUri, int port) {
    try (TDGContainer<TDGContainerImpl> c =
        TDGContainerImpl.builder(TDG_IMAGE).withAdvertiseUri(advertiseUri).build()) {
      c.start();
      final Integer portBeforeStop = c.getMappedPort(port);
      c.stopWithSafeMount();
      c.start();
      final Integer porAfterStop = c.getMappedPort(port);
      Assertions.assertEquals(portBeforeStop, porAfterStop);
    }
  }

  @RepeatedTest(3)
  void testMultipleRestartContainer() {
    try (TDGContainer<TDGContainerImpl> c = TDGContainerImpl.builder(TDG_IMAGE).build()) {
      c.start();
      c.stopWithSafeMount();
      c.start();
    }
  }
}

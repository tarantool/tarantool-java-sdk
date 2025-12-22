/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool.config.tarantool3;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HostPortTest {

  public static Stream<String> dataForTestInvalidHostPorts() {
    return Stream.of(
        "    localhost",
        "l:-10",
        ":  8080",
        "localhost:",
        "    hostname:8080",
        "hostname    :8080",
        "hostname:     8080",
        "hostname:8080    ",
        "hostname : 8080",
        "host@name:8080"
    );
  }


  @ParameterizedTest
  @MethodSource("dataForTestInvalidHostPorts")
  void testInvalidHostPorts(String hostPort) {
    Assertions.assertThrows(IllegalArgumentException.class, () -> HostPort.parse(hostPort));
  }

  public static Stream<Arguments> dataForTestHostPorts() {
    return Stream.of(
        Arguments.of("localhost:8080", new HostPort("localhost", 8080)),
        Arguments.of("127.0.0.1:3301", new HostPort("127.0.0.1", 3301)),
        Arguments.of("example.com:443", new HostPort("example.com", 443)),
        Arguments.of("db-server:5432", new HostPort("db-server", 5432)),
        Arguments.of("my-app.prod:9000", new HostPort("my-app.prod", 9000))
    );
  }

  @ParameterizedTest
  @MethodSource("dataForTestHostPorts")
  void testHostPorts(String hostPort, HostPort expectedHostPort) {
    Assertions.assertDoesNotThrow(() -> {
      final HostPort parse = HostPort.parse(hostPort);
      Assertions.assertEquals(expectedHostPort, parse);
    });
  }
}
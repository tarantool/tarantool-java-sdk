/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool.config.tarantool3;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.tarantool.config.ConfigurationUtils;

import io.tarantool.autogen.iproto.Iproto;

class ParameterExtractorTest {

  private static final ParameterExtractor<?> DEFAULT_PRIORITY_EXTRACTOR =
      new ParameterExtractor<>(
          c -> c.getIproto().flatMap(Iproto::getNetMsgMax),
          gr -> gr.getIproto().flatMap(i -> i.getNetMsgMax()),
          r -> r.getIproto().flatMap(i -> i.getNetMsgMax()),
          in -> in.getIproto().flatMap(i -> i.getNetMsgMax()),
          (gl, gr, r, i) -> {
            if (i.isPresent()) {
              return i;
            }
            if (r.isPresent()) {
              return r;
            }
            if (gr.isPresent()) {
              return gr;
            }
            return gl;
          });

  public static Stream<Arguments> dataForTestSimpleIproto() {
    final String globalPar =
        """
        groups:
          g-1:
            replicasets:
              r-1:
                instances:
                  i-1:
                    isolated: false
        iproto:
          net_msg_max: 685
        """;
    final String groupPar =
        """
        groups:
          g-1:
            iproto:
              net_msg_max: 700
            replicasets:
              r-1:
                instances:
                  i-1:
                    isolated: false
        iproto:
          net_msg_max: 685
        """;

    final String replicasetPar =
        """
        groups:
          g-1:
            iproto:
              net_msg_max: 700
            replicasets:
              r-1:
                iproto:
                  net_msg_max: 900
                instances:
                  i-1:
                    isolated: false
        iproto:
          net_msg_max: 685
        """;

    final String instancePar =
        """
        groups:
          g-1:
            iproto:
              net_msg_max: 700
            replicasets:
              r-1:
                iproto:
                  net_msg_max: 900
                instances:
                  i-1:
                    iproto:
                      net_msg_max: 1200
                    isolated: false
        iproto:
          net_msg_max: 685
        """;

    return Stream.of(
        Arguments.of(globalPar, BigInteger.valueOf(685), DEFAULT_PRIORITY_EXTRACTOR),
        Arguments.of(groupPar, BigInteger.valueOf(700), DEFAULT_PRIORITY_EXTRACTOR),
        Arguments.of(replicasetPar, BigInteger.valueOf(900), DEFAULT_PRIORITY_EXTRACTOR),
        Arguments.of(instancePar, BigInteger.valueOf(1200), DEFAULT_PRIORITY_EXTRACTOR));
  }

  @ParameterizedTest
  @MethodSource("dataForTestSimpleIproto")
  void testSimpleIproto(String raw, Object expectedResult, ParameterExtractor<?> extractor) {
    Assertions.assertDoesNotThrow(
        () -> {
          final var config = ConfigurationUtils.create(raw);
          final Map<String, ?> parameters = extractor.getParameter(config);

          parameters.forEach(
              (key, value) -> {
                Assertions.assertEquals(expectedResult, value);
                final Optional<?> parameter = extractor.getParameter(key, config);
                Assertions.assertEquals(Optional.ofNullable(expectedResult), parameter);
              });
        });
  }

  @Test
  void testNotFoundParameter() {
    final String instanceName = "i-1";
    final String withoutPramRaw =
        """
        groups:
          g-1:
            replicasets:
              r-1:
                instances:
                  %s:
                    isolated: false
        """
            .formatted(instanceName);

    final var config = ConfigurationUtils.create(withoutPramRaw);

    final Map<String, ?> parameters = DEFAULT_PRIORITY_EXTRACTOR.getParameter(config);
    Assertions.assertTrue(parameters.isEmpty());

    final Optional<?> parameter = DEFAULT_PRIORITY_EXTRACTOR.getParameter(instanceName, config);
    Assertions.assertEquals(Optional.empty(), parameter);
  }

  public static Stream<String> dataForTestInvalidConfigShouldThrow() {
    return Stream.of(
        // without groups
        " isolated: false",

        // without groups values
        """
        groups:
          g-1:
        """,

        // without replicaset values
        """
        groups:
          g-1:
            replicasets:
              r-1:
        """,

        // without replicasets
        """
        groups:
          g-1:
            isolated: false
        """,

        // without instances
        """
        groups:
          g-1:
            replicasets:
              r-1:
                isolated: false
        """);
  }

  @ParameterizedTest
  @MethodSource("dataForTestInvalidConfigShouldThrow")
  void testInvalidConfigShouldThrow(String raw) {
    Assertions.assertThrows(
        Exception.class,
        () -> DEFAULT_PRIORITY_EXTRACTOR.getParameter(ConfigurationUtils.create(raw)));
  }
}

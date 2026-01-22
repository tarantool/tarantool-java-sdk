/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool.config;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.tarantool.autogen.BoxCfg;
import io.tarantool.autogen.BoxCfg.BootstrapStrategy;

class LuaConfigurationTest {

  @TempDir static Path tempDir;

  public static Stream<Arguments> dataForTestLuaConfigurationSerialization() {
    final String format = "box.cfg{\n\t%s=%s,\n}";

    return Stream.of(
        // empty
        Arguments.of(tempDir.resolve("-" + UUID.randomUUID()), new BoxCfg(), "box.cfg{}"),

        // string
        Arguments.of(
            tempDir.resolve("-" + UUID.randomUUID()),
            BoxCfg.builder().withCustomProcTitle("hello").build(),
            String.format(format, "custom_proc_title", "'hello'")),

        // boolean
        Arguments.of(
            tempDir.resolve("-" + UUID.randomUUID()),
            BoxCfg.builder().withBackground(true).build(),
            String.format(format, "background", true)),

        // BigDecimal
        Arguments.of(
            tempDir.resolve("-" + UUID.randomUUID()),
            BoxCfg.builder().withFeedbackInterval(new BigDecimal("0.10000")).build(),
            String.format(format, "feedback_interval", 0.1)),

        // BigInteger
        Arguments.of(
            tempDir.resolve("-" + UUID.randomUUID()),
            BoxCfg.builder().withCheckpointCount(BigInteger.TEN).build(),
            String.format(format, "checkpoint_count", BigInteger.TEN)),

        // Enum
        Arguments.of(
            tempDir.resolve("-" + UUID.randomUUID()),
            BoxCfg.builder().withBootstrapStrategy(BootstrapStrategy.AUTO).build(),
            String.format(
                format, "bootstrap_strategy", "'" + BootstrapStrategy.AUTO.value() + "'")));
  }

  @ParameterizedTest
  @MethodSource("dataForTestLuaConfigurationSerialization")
  void TestLuaConfigurationSerialization(Path configPath, BoxCfg config, String expected)
      throws IOException {
    LuaConfiguration.writeAsLuaScript(configPath, config);
    Assertions.assertEquals(
        expected, new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8));
  }
}

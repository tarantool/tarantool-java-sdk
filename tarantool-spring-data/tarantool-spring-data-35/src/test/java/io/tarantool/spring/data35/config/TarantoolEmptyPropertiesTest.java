/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data35.config;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import io.tarantool.spring.data.utils.Constants;
import io.tarantool.spring.data35.config.properties.TarantoolProperties;
import io.tarantool.spring.data35.repository.config.EnableTarantoolRepositories;
import io.tarantool.spring.data35.utils.TarantoolTestSupport;

/**
 * @see TarantoolPropertiesTest
 */
@SpringBootTest(classes = {TarantoolEmptyPropertiesTest.Config.class})
@TestPropertySource(properties = {Constants.DEFAULT_PROPERTY_FILE_LOCATION_CLASSPATH})
@Timeout(5)
public class TarantoolEmptyPropertiesTest {

  @Autowired private TarantoolProperties springParsedProperties;

  @BeforeAll
  static void beforeAll() throws IOException {
    TarantoolTestSupport.writeTestEmptyPropertiesYaml(Constants.DEFAULT_PROPERTY_FILE_NAME);
  }

  /*
   * The test checks that if property processing is enabled via the @EnableConfigurationProperties annotation, then
   * TarantoolProperty always exists and has default values.
   */
  @Test
  void testParseEmptyProperties() {
    assertNotNull(springParsedProperties);
  }

  @AfterAll
  static void afterAll() throws IOException {
    Files.deleteIfExists(
        TarantoolTestSupport.DEFAULT_TEST_PROPERTY_DIR.resolve(
            Constants.DEFAULT_PROPERTY_FILE_NAME));
  }

  @EnableTarantoolRepositories
  static class Config {}
}

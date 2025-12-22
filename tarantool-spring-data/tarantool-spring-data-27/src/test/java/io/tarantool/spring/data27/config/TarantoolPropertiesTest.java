/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.config;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static io.tarantool.spring.data.utils.Constants.DEFAULT_PROPERTY_FILE_LOCATION_CLASSPATH;
import static io.tarantool.spring.data.utils.Constants.DEFAULT_PROPERTY_FILE_NAME;
import static io.tarantool.spring.data27.utils.TarantoolTestSupport.DEFAULT_TEST_PROPERTY_DIR;
import static io.tarantool.spring.data27.utils.TarantoolTestSupport.writeTestPropertiesYaml;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.spring.data27.config.properties.TarantoolProperties;
import io.tarantool.spring.data27.repository.config.EnableTarantoolRepositories;

/**
 * <p>The test checks whether the Spring framework parses a file with the specified properties correctly. Before
 * creating the test class, a random file with properties is written to the classpath:properties-test.yaml file. Next,
 * when creating a test class the Spring context is raised and parses the file with properties. A test is launched to
 * check the uniqueness of the current properties and properties that were specified in the previous iteration. It is
 * checked that the generated Spring class TarantoolProperty is equal to the class written to the file. Next, the next
 * entry to the file occurs, after which there is a forced reload of the Spring context -> the file with properties is
 * parsed again and everything happens according to circle.
 * </p>
 */
@SpringBootTest(classes = {TarantoolPropertiesTest.Config.class})
@TestPropertySource(properties = {DEFAULT_PROPERTY_FILE_LOCATION_CLASSPATH})
@Timeout(5)
public class TarantoolPropertiesTest {

  private static TarantoolProperties writtenToFileProperties;
  private static TarantoolProperties prevSpringParsedProperties;
  private static TarantoolProperties prevWrittenToFileProperties;
  @Autowired
  private TarantoolProperties springParsedProperties;

  @MockBean
  private TarantoolCrudClient client;

  @BeforeAll
  static void beforeAll() throws IOException {
    writtenToFileProperties = writeTestPropertiesYaml(DEFAULT_PROPERTY_FILE_NAME);
  }

  @AfterAll
  static void afterAll() throws IOException {
    Files.deleteIfExists(DEFAULT_TEST_PROPERTY_DIR.resolve(DEFAULT_PROPERTY_FILE_NAME));
  }

  /**
   * The annotation allows you to tell Spring that the context is dirty and needs to be reloaded after the test.
   */
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  @RepeatedTest(10)
  void testParseProperty() throws IOException {
    assertNotEquals(prevWrittenToFileProperties, writtenToFileProperties);
    assertNotEquals(prevSpringParsedProperties, springParsedProperties);
    assertEquals(writtenToFileProperties, springParsedProperties);

    prevWrittenToFileProperties = writtenToFileProperties;
    prevSpringParsedProperties = springParsedProperties;

    // Write the Properties class that will be parsed by Spring in the next iteration.
    writtenToFileProperties = writeTestPropertiesYaml(DEFAULT_PROPERTY_FILE_NAME);
  }

  @EnableTarantoolRepositories
  static class Config {}
}



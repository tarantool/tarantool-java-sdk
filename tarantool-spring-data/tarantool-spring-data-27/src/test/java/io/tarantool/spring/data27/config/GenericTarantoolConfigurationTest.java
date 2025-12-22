/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.config;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.TestPropertySource;

import static io.tarantool.spring.data.utils.Constants.DEFAULT_PROPERTY_FILE_LOCATION_CLASSPATH;
import static io.tarantool.spring.data.utils.Constants.DEFAULT_PROPERTY_FILE_NAME;
import static io.tarantool.spring.data27.utils.TarantoolTestSupport.DEFAULT_TEST_PROPERTY_DIR;
import static io.tarantool.spring.data27.utils.TarantoolTestSupport.writeTestPropertiesYaml;
import io.tarantool.spring.data27.config.properties.TarantoolProperties;

@TestPropertySource(properties = {DEFAULT_PROPERTY_FILE_LOCATION_CLASSPATH})
public abstract class GenericTarantoolConfigurationTest implements ApplicationContextAware {

  protected static TarantoolProperties testProperties;

  protected ApplicationContext applicationContext;

  @Autowired protected TarantoolProperties properties;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @BeforeAll
  static void beforeAll() throws IOException {
    testProperties = writeTestPropertiesYaml(DEFAULT_PROPERTY_FILE_NAME);
  }

  @AfterAll
  static void afterAll() throws IOException {
    Files.deleteIfExists(DEFAULT_TEST_PROPERTY_DIR.resolve(DEFAULT_PROPERTY_FILE_NAME));
  }
}

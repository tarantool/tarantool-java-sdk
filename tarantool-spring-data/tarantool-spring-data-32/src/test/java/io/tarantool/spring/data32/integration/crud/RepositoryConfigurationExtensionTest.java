/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.integration.crud;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import static io.tarantool.spring.data.TarantoolBeanNames.DEFAULT_TARANTOOL_CRUD_CLIENT_BEAN_REF;
import static io.tarantool.spring.data.TarantoolBeanNames.DEFAULT_TARANTOOL_CRUD_KEY_VALUE_ADAPTER_REF;
import static io.tarantool.spring.data.TarantoolBeanNames.DEFAULT_TARANTOOL_KEY_VALUE_TEMPLATE_REF;
import io.tarantool.client.ClientType;
import io.tarantool.client.factory.TarantoolBoxClientBuilder;
import io.tarantool.client.factory.TarantoolCrudClientBuilder;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.spring.data32.integration.BaseIntegrationTest;
import io.tarantool.spring.data32.repository.config.EnableTarantoolRepositories;
import io.tarantool.spring.data32.repository.config.TarantoolRepositoryConfigurationExtension;

class RepositoryConfigurationExtensionTest extends BaseIntegrationTest {

  private static TarantoolRepositoryConfigurationExtension defaultExtension;

  @BeforeAll
  static void setUp() throws IOException {
    BaseIntegrationTest.beforeAll();
    defaultExtension = new TarantoolRepositoryConfigurationExtension();
  }

  @Test
  void testGetModuleName() {
    assertEquals("Tarantool", defaultExtension.getModuleName());
  }

  @Test
  void testGetModulePrefix() {
    assertEquals("tarantool", defaultExtension.getModulePrefix());
  }

  @Test
  void testGetDefaultKeyValueTemplateRef() {
    assertEquals(DEFAULT_TARANTOOL_KEY_VALUE_TEMPLATE_REF, defaultExtension.getDefaultKeyValueTemplateRef());
  }

  @Test
  void testRegisterBeansForRootWithDefaultConfig() {
    final ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    assertTrue(context.containsBean(DEFAULT_TARANTOOL_CRUD_CLIENT_BEAN_REF));
    assertTrue(context.containsBean(DEFAULT_TARANTOOL_CRUD_KEY_VALUE_ADAPTER_REF));
    assertTrue(context.containsBean(DEFAULT_TARANTOOL_KEY_VALUE_TEMPLATE_REF));
  }

  @Test
  void testRegisterBeansForRootWithCustomClientConfigShouldThrows() {
    final Throwable throwable = assertThrows(IllegalArgumentException.class,
        () -> new AnnotationConfigApplicationContext(BoxNonSupportConfig.class));

    final String exceptionMessage = "The Box client is not yet supported.";
    assertEquals(exceptionMessage, throwable.getMessage());
  }

  private static TarantoolCrudClientBuilder getCrudClientSettings() {
    return TarantoolFactory.crud()
        .withHost(getHost())
        .withPort(getPort());
  }

  private static TarantoolBoxClientBuilder getBoxClientSettings() {
    return TarantoolFactory.box()
        .withHost(getHost())
        .withPort(getPort());
  }

  @EnableTarantoolRepositories
  static class DefaultConfig {

    @Bean
    public TarantoolCrudClientBuilder clientSettings() {
      return getCrudClientSettings();
    }
  }

  @EnableTarantoolRepositories(clientType = ClientType.BOX)
  static class BoxNonSupportConfig {

    @Bean
    public TarantoolBoxClientBuilder clientSettings() {
      return getBoxClientSettings();
    }
  }
}

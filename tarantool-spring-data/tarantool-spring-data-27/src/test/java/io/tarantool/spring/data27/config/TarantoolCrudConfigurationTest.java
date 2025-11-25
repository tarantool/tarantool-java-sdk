/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static io.tarantool.spring.data.TarantoolBeanNames.DEFAULT_TARANTOOL_KEY_VALUE_TEMPLATE_REF;
import static io.tarantool.spring.data.utils.Constants.DEFAULT_PROPERTY_FILE_NAME;
import static io.tarantool.spring.data27.utils.TarantoolTestSupport.writeTestPropertiesYaml;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolCrudClientBuilder;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.spring.data.config.properties.BaseTarantoolProperties.PropertyHeartbeatOpts;
import io.tarantool.spring.data.config.properties.BaseTarantoolProperties.PropertyInstanceConnectionGroup;
import io.tarantool.spring.data27.repository.config.EnableTarantoolRepositories;

@SpringBootTest(classes = {TarantoolCrudConfigurationTest.Config.class})
public class TarantoolCrudConfigurationTest extends GenericTarantoolConfigurationTest {

  @MockBean TarantoolCrudClient client;

  @Autowired private TarantoolCrudConfiguration tarantoolCrudConfiguration;

  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  @RepeatedTest(10)
  void testGetCrudClientBuilder() throws IOException {
    assertEquals(properties, tarantoolCrudConfiguration.getProperties());

    final TarantoolCrudClientBuilder crudClientBuilder =
        tarantoolCrudConfiguration.getClientBuilder();

    if (properties.getConnectionGroups() == null) {
      assertNull(crudClientBuilder.getGroups());
    } else {
      assertEquals(properties.getConnectionGroups().size(), crudClientBuilder.getGroups().size());
      List<InstanceConnectionGroup> connectionGroupsFromBuilder = crudClientBuilder.getGroups();
      List<PropertyInstanceConnectionGroup> connectionGroupsFromProperty =
          properties.getConnectionGroups();
      assertNotNull(connectionGroupsFromBuilder);
      assertNotNull(connectionGroupsFromProperty);
      for (int i = 0; i < connectionGroupsFromBuilder.size(); i++) {
        InstanceConnectionGroup groupFromBuilder = connectionGroupsFromBuilder.get(i);
        PropertyInstanceConnectionGroup groupFromProperty = connectionGroupsFromProperty.get(i);

        assertEquals(groupFromProperty.getAuthType(), groupFromBuilder.getAuthType());
        assertEquals(groupFromProperty.getTag(), groupFromBuilder.getTag());
        assertEquals(groupFromProperty.getConnectionGroupSize(), groupFromBuilder.getSize());
        assertEquals(groupFromProperty.getUserName(), groupFromBuilder.getUser());
        assertEquals(groupFromProperty.getPassword(), groupFromBuilder.getPassword());
        assertEquals(groupFromProperty.getPort(), groupFromBuilder.getPort());
        assertEquals(groupFromProperty.getHost(), groupFromBuilder.getHost());
      }
    }

    final PropertyHeartbeatOpts propertiesHeartbeatOpts = properties.getHeartbeat();
    if (propertiesHeartbeatOpts == null) {
      assertNull(crudClientBuilder.getHeartbeatOpts());
    } else {
      HeartbeatOpts builderHeartbeatOpts = crudClientBuilder.getHeartbeatOpts();

      assertEquals(
          propertiesHeartbeatOpts.getPingInterval(), builderHeartbeatOpts.getPingInterval());
      assertEquals(
          propertiesHeartbeatOpts.getDeathThreshold(), builderHeartbeatOpts.getDeathThreshold());
      assertEquals(
          propertiesHeartbeatOpts.getInvalidationThreshold(),
          builderHeartbeatOpts.getInvalidationThreshold());
      assertEquals(propertiesHeartbeatOpts.getWindowSize(), builderHeartbeatOpts.getWindowSize());
    }

    assertEquals(properties.getUserName(), crudClientBuilder.getUser());
    assertEquals(properties.getPort(), crudClientBuilder.getPort());
    assertEquals(properties.getEventLoopThreadsCount(), crudClientBuilder.getnThreads());
    assertEquals(properties.getBalancerClass(), crudClientBuilder.getBalancerClass());
    assertEquals(properties.isGracefulShutdownEnabled(), crudClientBuilder.isGracefulShutdown());
    assertEquals(properties.getConnectTimeout(), crudClientBuilder.getConnectTimeout());
    assertEquals(properties.getReconnectAfter(), crudClientBuilder.getReconnectAfter());
    assertEquals(properties.getHost(), crudClientBuilder.getHost());
    assertEquals(properties.getPassword(), crudClientBuilder.getPassword());

    testProperties = writeTestPropertiesYaml(DEFAULT_PROPERTY_FILE_NAME);
  }

  @Test
  void testKeyValueTemplate() {
    final List<String> beanDefinitions = Arrays.asList(applicationContext.getBeanDefinitionNames());
    assertTrue(beanDefinitions.contains(DEFAULT_TARANTOOL_KEY_VALUE_TEMPLATE_REF));

    final KeyValueTemplate keyValueTemplate =
        applicationContext.getBean(
            DEFAULT_TARANTOOL_KEY_VALUE_TEMPLATE_REF, KeyValueTemplate.class);
    assertNotNull(keyValueTemplate);
  }

  @EnableTarantoolRepositories
  static class Config {}
}

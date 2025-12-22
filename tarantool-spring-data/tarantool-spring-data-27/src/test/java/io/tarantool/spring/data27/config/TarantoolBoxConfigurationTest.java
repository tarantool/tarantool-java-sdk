/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.config;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static io.tarantool.spring.data.utils.Constants.DEFAULT_PROPERTY_FILE_NAME;
import static io.tarantool.spring.data27.utils.TarantoolTestSupport.writeTestPropertiesYaml;
import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.factory.TarantoolBoxClientBuilder;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.spring.data27.config.properties.TarantoolProperties;

@SpringBootTest(classes = {TarantoolBoxConfigurationTest.Config.class})
public class TarantoolBoxConfigurationTest extends GenericTarantoolConfigurationTest {

  @MockBean TarantoolBoxClient client;

  @Autowired private TarantoolBoxConfiguration tarantoolBoxConfiguration;

  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  @RepeatedTest(10)
  void testGetBoxClientBuilder() throws IOException {
    assertEquals(properties, tarantoolBoxConfiguration.getProperties());

    final TarantoolBoxClientBuilder boxClientBuilder = tarantoolBoxConfiguration.getClientBuilder();

    if (properties.getConnectionGroups() == null) {
      assertNull(boxClientBuilder.getGroups());
    } else {
      assertEquals(properties.getConnectionGroups().size(), boxClientBuilder.getGroups().size());
      List<InstanceConnectionGroup> connectionGroupsFromBuilder = boxClientBuilder.getGroups();
      List<InstanceConnectionGroup> connectionGroupsFromProperty =
          properties.getInstanceConnectionGroups();

      assertNotNull(connectionGroupsFromBuilder);
      assertNotNull(connectionGroupsFromProperty);
      for (int i = 0; i < connectionGroupsFromBuilder.size(); i++) {
        InstanceConnectionGroup groupFromBuilder = connectionGroupsFromBuilder.get(i);
        InstanceConnectionGroup groupFromProperty = connectionGroupsFromProperty.get(i);

        assertEquals(groupFromProperty.getAuthType(), groupFromBuilder.getAuthType());
        assertEquals(groupFromProperty.getTag(), groupFromBuilder.getTag());
        assertEquals(groupFromProperty.getSize(), groupFromBuilder.getSize());
        assertEquals(groupFromProperty.getUser(), groupFromBuilder.getUser());
        assertEquals(groupFromProperty.getPassword(), groupFromBuilder.getPassword());
        assertEquals(groupFromProperty.getPort(), groupFromBuilder.getPort());
        assertEquals(groupFromProperty.getHost(), groupFromBuilder.getHost());
      }
    }

    final HeartbeatOpts propertiesHeartbeatOpts = properties.getHeartbeatOpts();
    if (propertiesHeartbeatOpts == null) {
      assertNull(boxClientBuilder.getHeartbeatOpts());
    } else {
      HeartbeatOpts builderHeartbeatOpts = boxClientBuilder.getHeartbeatOpts();
      assertEquals(
          propertiesHeartbeatOpts.getPingInterval(), builderHeartbeatOpts.getPingInterval());
      assertEquals(
          propertiesHeartbeatOpts.getDeathThreshold(), builderHeartbeatOpts.getDeathThreshold());
      assertEquals(
          propertiesHeartbeatOpts.getInvalidationThreshold(),
          builderHeartbeatOpts.getInvalidationThreshold());
      assertEquals(propertiesHeartbeatOpts.getWindowSize(), builderHeartbeatOpts.getWindowSize());
    }

    assertEquals(properties.getUserName(), boxClientBuilder.getUser());
    assertEquals(properties.getPort(), boxClientBuilder.getPort());
    assertEquals(properties.getEventLoopThreadsCount(), boxClientBuilder.getnThreads());
    assertEquals(properties.getBalancerClass(), boxClientBuilder.getBalancerClass());
    assertEquals(properties.isGracefulShutdownEnabled(), boxClientBuilder.isGracefulShutdown());
    assertEquals(properties.getConnectTimeout(), boxClientBuilder.getConnectTimeout());
    assertEquals(properties.getReconnectAfter(), boxClientBuilder.getReconnectAfter());
    assertEquals(properties.getHost(), boxClientBuilder.getHost());
    assertEquals(properties.getPassword(), boxClientBuilder.getPassword());
    assertEquals(properties.isFetchSchema(), boxClientBuilder.isFetchSchema());
    assertEquals(
        properties.isIgnoreOldSchemaVersion(), boxClientBuilder.isIgnoreOldSchemaVersion());

    testProperties = writeTestPropertiesYaml(DEFAULT_PROPERTY_FILE_NAME);
  }

  @Configuration
  @EnableConfigurationProperties(TarantoolProperties.class)
  @Import(TarantoolBoxConfiguration.class)
  static class Config {}
}

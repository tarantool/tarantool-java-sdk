/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.integration.crud;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import static io.tarantool.client.TarantoolClient.DEFAULT_TAG;
import static io.tarantool.client.crud.TarantoolCrudClient.DEFAULT_CRUD_PASSWORD;
import static io.tarantool.client.crud.TarantoolCrudClient.DEFAULT_CRUD_USERNAME;
import static io.tarantool.spring.data.utils.Constants.DEFAULT_PROPERTY_FILE_LOCATION_CLASSPATH;
import static io.tarantool.spring.data31.utils.TarantoolTestSupport.COMPLEX_PERSON_SPACE;
import static io.tarantool.spring.data31.utils.TarantoolTestSupport.PERSON_SPACE;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.client.factory.TarantoolCrudClientBuilder;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.spring.data31.integration.BaseIntegrationTest;
import io.tarantool.spring.data31.repository.config.EnableTarantoolRepositories;

@TestPropertySource(properties = {DEFAULT_PROPERTY_FILE_LOCATION_CLASSPATH})
abstract class CrudConfigurations extends BaseIntegrationTest {

  @Autowired protected TarantoolCrudClient client;

  private static InstanceConnectionGroup groupFromJavaConfig;

  private static final String PACKAGE_PATH = "io.tarantool.spring.data31.utils.core";

  @BeforeAll
  protected static void beforeAll() throws IOException {
    BaseIntegrationTest.beforeAll();
    groupFromJavaConfig =
        InstanceConnectionGroup.builder()
            .withHost(clusterContainer.getHost())
            .withPort(clusterContainer.getPort())
            .withUser(DEFAULT_CRUD_USERNAME)
            .withPassword(DEFAULT_CRUD_PASSWORD)
            .withTag(DEFAULT_TAG)
            .withSize(4)
            .build();
  }

  @BeforeEach
  public void truncateSpaces() {
    client.space(PERSON_SPACE).truncate().join();
    client.space(COMPLEX_PERSON_SPACE).truncate().join();
  }

  @EnableTarantoolRepositories(basePackages = {PACKAGE_PATH})
  @Configuration
  static class JavaConfigConfiguration {

    @Bean
    TarantoolCrudClientBuilder crudClientBuilder() {
      return TarantoolFactory.crud().withGroups(Collections.singletonList(groupFromJavaConfig));
    }
  }

  @EnableTarantoolRepositories(basePackages = {PACKAGE_PATH})
  @Configuration
  static class ViaPropertyFileConfiguration {}

  @Test
  void testSimpleClient() {
    TarantoolCrudSpace space = client.space(PERSON_SPACE);

    assertEquals(0, space.select().join().size());

    List<?> tuple = Arrays.asList(0, true, "petya");
    List<?> insertTuple = space.insert(tuple).join().get();

    assertEquals(tuple, insertTuple.subList(0, insertTuple.size() - 1));
  }
}

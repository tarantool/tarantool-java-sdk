/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.integration.crud;

import org.springframework.boot.test.context.SpringBootTest;

import io.tarantool.spring.data33.integration.crud.CrudConfigurations.ViaPropertyFileConfiguration;

@SpringBootTest(classes = {ViaPropertyFileConfiguration.class})
class RepositoryViaPropertiesTest extends GenericRepositoryTest {}

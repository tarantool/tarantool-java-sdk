/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.integration.crud;

import org.springframework.boot.test.context.SpringBootTest;

import io.tarantool.spring.data31.integration.crud.CrudConfigurations.ViaPropertyFileConfiguration;

@SpringBootTest(classes = {ViaPropertyFileConfiguration.class})
class TarantoolTemplateViaPropertiesTest extends GenericTarantoolTemplateTest {}

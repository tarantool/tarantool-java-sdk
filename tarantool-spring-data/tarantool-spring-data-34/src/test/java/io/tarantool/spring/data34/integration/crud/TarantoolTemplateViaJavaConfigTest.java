/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.integration.crud;

import org.springframework.boot.test.context.SpringBootTest;

import io.tarantool.spring.data34.integration.crud.CrudConfigurations.JavaConfigConfiguration;

@SpringBootTest(classes = {JavaConfigConfiguration.class})
class TarantoolTemplateViaJavaConfigTest extends GenericTarantoolTemplateTest {}

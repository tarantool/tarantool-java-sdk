/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.integration.crud;

import org.springframework.boot.test.context.SpringBootTest;

import io.tarantool.spring.data33.integration.crud.CrudConfigurations.JavaConfigConfiguration;

@SpringBootTest(classes = {JavaConfigConfiguration.class})
class TarantoolTemplateViaJavaConfigTest extends GenericTarantoolTemplateTest {}

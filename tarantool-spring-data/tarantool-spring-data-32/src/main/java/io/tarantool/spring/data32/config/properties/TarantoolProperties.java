/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.tarantool.spring.data.config.properties.BaseTarantoolProperties;

/**
 * Configuration properties for Tarantool.
 *
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
@ConfigurationProperties(prefix = "spring.data.tarantool")
public class TarantoolProperties extends BaseTarantoolProperties {}

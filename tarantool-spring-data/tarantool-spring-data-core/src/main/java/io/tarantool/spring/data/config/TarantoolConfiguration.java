/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data.config;

import io.tarantool.spring.data.config.properties.BaseTarantoolProperties;

/**
 * Abstract class for configuration of various clients, such as CRUD or BOX. Aggregates default
 * values for settings specific to the Spring Data layer.
 */
public abstract class TarantoolConfiguration<T> {

  protected final BaseTarantoolProperties properties;

  public TarantoolConfiguration(BaseTarantoolProperties properties) {
    this.properties = properties;
  }

  public BaseTarantoolProperties getProperties() {
    return properties;
  }

  abstract T getClientBuilder();
}

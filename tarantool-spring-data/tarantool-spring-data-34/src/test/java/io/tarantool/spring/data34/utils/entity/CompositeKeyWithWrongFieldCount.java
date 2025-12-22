/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.utils.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.tarantool.spring.data.mapping.model.CompositeKey;

public class CompositeKeyWithWrongFieldCount implements CompositeKey {

  @JsonProperty("id")
  private long id;
}

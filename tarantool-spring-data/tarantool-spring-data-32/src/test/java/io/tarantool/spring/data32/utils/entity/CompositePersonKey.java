/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.utils.entity;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.tarantool.spring.data.mapping.model.CompositeKey;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompositePersonKey implements CompositeKey {

  @JsonProperty("id")
  private Integer id;

  @JsonProperty("secondId")
  private UUID secondId;
}

/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.utils.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import io.tarantool.spring.data.core.annotation.IdClass;

@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(CompositeKeyWithWrongFieldCount.class)
public class EntityWithWrongCompositeKeyPartsCount {

  @Id
  @JsonProperty("id")
  private long id;

  @Id
  @JsonProperty("secondId")
  private long secondId;
}

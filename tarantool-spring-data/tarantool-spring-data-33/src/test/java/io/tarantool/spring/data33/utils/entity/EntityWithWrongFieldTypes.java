/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.utils.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import io.tarantool.spring.data.core.annotation.IdClass;

@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(CompositeKeyWithWrongFieldTypes.class)
public class EntityWithWrongFieldTypes {

  @Id
  @JsonProperty("id")
  private long id;

  @Id
  @JsonProperty("secondId")
  private String secondId;

  @Id
  @JsonProperty("thirdId")
  private int thirdId;
}

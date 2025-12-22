/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.utils.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.tarantool.spring.data.mapping.model.CompositeKey;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompositeKeyWithWrongFieldTypes implements CompositeKey {

  @JsonProperty("id")
  private long id;

  @JsonProperty("secondId")
  private String secondId;

  @JsonProperty("thirdId")
  private boolean thirdId;
}

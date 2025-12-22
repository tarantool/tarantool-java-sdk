/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.utils.entity;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompositePersonKeyWithoutJsonFormat {

  @JsonProperty("id")
  private Integer id;

  @JsonProperty("secondId")
  private UUID secondId;
}

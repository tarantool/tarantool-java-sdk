/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Artyom Dubinin
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TestEntity {

  @JsonProperty("id")
  public char[] id;

  @JsonProperty("value")
  public String value;
}

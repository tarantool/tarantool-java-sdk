/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Artyom Dubinin
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonIgnoreProperties(ignoreUnknown = true) // for example bucket_id
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder(toBuilder = true)
public class Person {

  @JsonProperty("id")
  public Integer id;

  @JsonProperty("isMarried")
  public Boolean isMarried;

  @JsonProperty("name")
  public String name;

  public List<Object> asList() {
    return new ArrayList<>(Arrays.asList(id, isMarried, name));
  }

  public List<Object> asList(List<Object> additional) {
    ArrayList<Object> result = new ArrayList<>(Arrays.asList(id, isMarried, name));
    result.addAll(additional);
    return result;
  }
}

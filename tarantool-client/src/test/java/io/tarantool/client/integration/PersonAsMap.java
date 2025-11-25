/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.integration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true) // for example bucket_id
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder(toBuilder = true)
@JsonFormat(shape = Shape.OBJECT)
public class PersonAsMap {

  private static final String idFieldName = "id";
  private static final String isMarriedFieldName = "is_married";
  private static final String nameFieldName = "name";

  @JsonProperty(idFieldName)
  public Integer id;

  @JsonProperty(isMarriedFieldName)
  public Boolean isMarried;

  @JsonProperty(nameFieldName)
  public String name;

  public Map<String, Object> asMap() {
    return new HashMap<String, Object>() {
      {
        put(idFieldName, id);
        put(isMarriedFieldName, isMarried);
        put(nameFieldName, name);
      }
    };
  }

  public List<?> asList() {
    return Arrays.asList(id, isMarried, name);
  }
}

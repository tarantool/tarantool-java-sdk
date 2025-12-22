/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.integration;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonIgnoreProperties(ignoreUnknown = true) // for example bucket_id
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder(toBuilder = true)
public class NestedPerson {

  @JsonProperty("id")
  private int id;

  @JsonProperty("name")
  private String name;

  // like array
  @JsonProperty("friends")
  private List<Person> friends;

  // like map
  @JsonProperty("buys")
  private Map<String, Object> buys;

  // like array
  @JsonProperty("husband")
  private Person husband;

  // like map
  @JsonProperty("child")
  private Child child;
}


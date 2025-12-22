/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.utils.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

import io.tarantool.spring.data.utils.GenericPerson;
import io.tarantool.spring.data27.query.Field;

/**
 * @author Artyom Dubinin
 */
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonIgnoreProperties(ignoreUnknown = true) // for example bucket_id
@Data
@KeySpace("person")
public class Person implements GenericPerson<Integer> {

  @Id
  @Field("id")
  @JsonProperty("id")
  public Integer id;

  @Field("is_married")
  @JsonProperty("isMarried")
  private Boolean isMarried;

  @JsonProperty("name")
  private String name;

  @Override
  public Integer generateFullKey() {
    return id;
  }
}

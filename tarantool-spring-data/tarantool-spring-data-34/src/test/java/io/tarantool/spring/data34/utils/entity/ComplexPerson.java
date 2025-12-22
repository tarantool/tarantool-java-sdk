/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.utils.entity;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

import io.tarantool.spring.data.core.annotation.IdClass;
import io.tarantool.spring.data.utils.GenericPerson;
import io.tarantool.spring.data34.query.Field;

@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonIgnoreProperties(ignoreUnknown = true) // for example bucket_id
@Data
@KeySpace("complex_person")
@IdClass(CompositePersonKey.class)
public class ComplexPerson implements GenericPerson<CompositePersonKey> {

  @Id
  @Field("id")
  @JsonProperty("id")
  private Integer id;

  @Id
  @Field("second_id")
  @JsonProperty("secondId")
  private UUID secondId;

  @Field("is_married")
  @JsonProperty("isMarried")
  protected Boolean isMarried;

  @Field("name")
  @JsonProperty("name")
  String name;

  @Override
  public CompositePersonKey generateFullKey() {
    return new CompositePersonKey(id, secondId);
  }
}

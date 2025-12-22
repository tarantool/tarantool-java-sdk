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
import io.tarantool.spring.data34.query.Field;

@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonIgnoreProperties(ignoreUnknown = true) // for example bucket_id
@Data
@KeySpace("complex_person")
@IdClass(CompositePersonKeyWithJsonFormat.class)
public class ComplexPersonWithJsonFormatVariantPK {

  @Id
  @JsonProperty("id")
  private Integer id;

  @Id
  @Field("second_id")
  @JsonProperty("secondId")
  private UUID secondId;

  @Field("is_married")
  @JsonProperty("isMarried")
  private Boolean isMarried;

  @JsonProperty("name")
  private String name;
}

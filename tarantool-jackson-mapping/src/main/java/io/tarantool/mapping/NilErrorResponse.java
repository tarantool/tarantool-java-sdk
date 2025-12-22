/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class NilErrorResponse<T, V> {

  private final T response;

  private final V error;

  @JsonCreator
  public NilErrorResponse(@JsonProperty("response") T response, @JsonProperty("error") V error) {
    this.response = response;
    this.error = error;
  }

  public T get() {
    if (response == null && error != null) {
      throw new RuntimeException(error.toString());
    }
    return response;
  }

  @Override
  public String toString() {
    return "NilErrorResponse{" + "response=" + response + ", error=" + error + '}';
  }
}

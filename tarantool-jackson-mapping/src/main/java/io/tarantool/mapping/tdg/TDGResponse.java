/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.tdg;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.tarantool.core.exceptions.ClientException;
import io.tarantool.mapping.slash.errors.TarantoolSlashErrors;
import io.tarantool.mapping.slash.errors.TarantoolSlashErrorsException;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class TDGResponse<T> {

  private final T data;

  private final TarantoolSlashErrors error;

  @JsonCreator
  public TDGResponse(
      @JsonProperty("response") T data, @JsonProperty("error") TarantoolSlashErrors error) {
    this.data = data;
    this.error = error;
  }

  public T getData() throws ClientException {
    if (error != null) {
      throw new TarantoolSlashErrorsException(error);
    }
    return data;
  }
}

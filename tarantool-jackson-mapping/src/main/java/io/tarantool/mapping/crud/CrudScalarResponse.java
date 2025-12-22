/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.crud;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The class implements scalar response for the CRUD operations.
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see CrudError
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class CrudScalarResponse<T> {

  /** Scalar value of response. */
  private final T value;

  /** {@link CrudError} object returned from Tarantool. */
  private final CrudError error;

  /**
   * Creates a {@link CrudScalarResponse} object with the given parameters.
   *
   * @param value scalar value of response
   * @param error {@link CrudError} object
   */
  @JsonCreator
  public CrudScalarResponse(
      @JsonProperty("value") T value, @JsonProperty("error") CrudError error) {
    this.value = value;
    this.error = error;
  }

  /**
   * Returns scalar value of response.
   *
   * @return {@link #value} value.
   */
  public T getValue() {
    if (error != null) {
      throw new CrudException(error);
    }
    return value;
  }
}

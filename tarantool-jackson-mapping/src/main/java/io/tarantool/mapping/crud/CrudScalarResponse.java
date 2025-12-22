/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping.crud;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p> The class implements scalar response for the CRUD operations.</p>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see CrudError
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class CrudScalarResponse<T> {

  /**
   * <p>Scalar value of response.</p>
   */
  private final T value;

  /**
   * <p>{@link CrudError} object returned from Tarantool.</p>
   */
  private final CrudError error;

  /**
   * <p>Creates a {@link CrudScalarResponse} object with the given parameters.</p>
   *
   * @param value scalar value of response
   * @param error {@link CrudError} object
   */
  @JsonCreator
  public CrudScalarResponse(
      @JsonProperty("value") T value,
      @JsonProperty("error") CrudError error) {
    this.value = value;
    this.error = error;
  }

  /**
   * <p>Returns scalar value of response.</p>
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

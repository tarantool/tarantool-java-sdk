/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.crud;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The class is designed to deserialize and present the response as Java objects for CRUD {@code
 * insertMany(List) insertMany(...)}, {@code replaceMany(List) replaceMany(...)}, {@code
 * upsertMany(List) upsertMany(...)} operations.
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see CrudError
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class CrudBatchResponse<T> {

  /** Rows of tuples on which the action is performed. */
  private final T rows;

  /** List of errors encountered during the operation. */
  private final List<CrudError> errors;

  /**
   * Creates a {@link CrudBatchResponse} object with the given parameters.
   *
   * @param response {@link CrudData} object
   * @param errors list of {@link CrudError} objects
   */
  @JsonCreator
  public CrudBatchResponse(
      @JsonProperty("response") CrudData<T> response,
      @JsonProperty("errors") List<CrudError> errors) {
    this.rows = response == null ? null : response.getRows();
    this.errors = errors;
  }

  /**
   * Returns value of rows field.
   *
   * @return {@link #rows} value.
   */
  public T getRows() {
    return rows;
  }

  /**
   * Returns value of errors field.
   *
   * @return list of {@link CrudError} objects.
   */
  public List<CrudError> getErrors() {
    return errors;
  }
}

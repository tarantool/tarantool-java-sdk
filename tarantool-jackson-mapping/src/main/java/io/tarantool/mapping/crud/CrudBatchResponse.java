/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping.crud;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p> The class is designed to deserialize and present the response as Java objects for CRUD
 * {@code insertMany(List) insertMany(...)},
 * {@code replaceMany(List) replaceMany(...)},
 * {@code upsertMany(List) upsertMany(...)} operations.</p>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see CrudError
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class CrudBatchResponse<T> {

  /**
   * <p>Rows of tuples on which the action is performed.</p>
   */
  private final T rows;

  /**
   * <p>List of errors encountered during the operation.</p>
   */
  private final List<CrudError> errors;

  /**
   * <p>Creates a {@link CrudBatchResponse} object with the given parameters.</p>
   *
   * @param response {@link CrudData} object
   * @param errors   list of {@link CrudError} objects
   */
  @JsonCreator
  public CrudBatchResponse(
      @JsonProperty("response") CrudData<T> response,
      @JsonProperty("errors") List<CrudError> errors) {
    this.rows = response == null ? null : response.getRows();
    this.errors = errors;
  }

  /**
   * <p>Returns value of rows field.</p>
   *
   * @return {@link #rows} value.
   */
  public T getRows() {
    return rows;
  }

  /**
   * <p>Returns value of errors field.</p>
   *
   * @return list of {@link CrudError} objects.
   */
  public List<CrudError> getErrors() {
    return errors;
  }
}

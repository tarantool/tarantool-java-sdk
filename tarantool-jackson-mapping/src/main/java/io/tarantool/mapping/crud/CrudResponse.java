/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.crud;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.tarantool.mapping.Field;

/**
 * The class is designed to deserialize and present the response as Java objects for CRUD
 * operations.
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see CrudError
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class CrudResponse<T> {

  /** {@link CrudData} object returned from Tarantool. */
  private final CrudData<T> response;

  /** {@link CrudError} object returned from Tarantool. */
  private final CrudError error;

  /**
   * Creates a {@link CrudResponse} object with the given parameters.
   *
   * @param response {@link CrudData} object
   * @param error {@link CrudError} object
   */
  @JsonCreator
  public CrudResponse(
      @JsonProperty("response") CrudData<T> response, @JsonProperty("error") CrudError error) {
    this.response = response;
    this.error = error;
  }

  /**
   * Returns data rows.
   *
   * @return {@link CrudData#rows} value.
   * @throws CrudException when {@code error != null}
   */
  public T getRows() throws CrudException {
    if (error != null) {
      throw new CrudException(error);
    }
    if (response == null) {
      return null;
    }
    return response.getRows();
  }

  /**
   * Returns metadata fields.
   *
   * @return a list of {@link Field} objects representing the metadata.
   * @throws CrudException when {@code error != null}
   */
  public List<Field> getMetadata() throws CrudException {
    if (error != null) {
      throw new CrudException(error);
    }
    return response.getMetadata();
  }

  @Override
  public String toString() {
    return "CrudResponse{" + "response=" + response + ", error=" + error + '}';
  }
}

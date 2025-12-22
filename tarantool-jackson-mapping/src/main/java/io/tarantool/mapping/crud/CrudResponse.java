/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping.crud;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.tarantool.mapping.Field;

/**
 * <p> The class is designed to deserialize and present the response as Java objects for CRUD operations.</p>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see CrudError
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class CrudResponse<T> {

  /**
   * <p>{@link CrudData} object returned from Tarantool.</p>
   */
  private final CrudData<T> response;

  /**
   * <p>{@link CrudError} object returned from Tarantool.</p>
   */
  private final CrudError error;

  /**
   * <p>Creates a {@link CrudResponse} object with the given parameters.</p>
   *
   * @param response {@link CrudData} object
   * @param error    {@link CrudError} object
   */
  @JsonCreator
  public CrudResponse(
      @JsonProperty("response") CrudData<T> response,
      @JsonProperty("error") CrudError error) {
    this.response = response;
    this.error = error;
  }

  /**
   * <p>Returns data rows.</p>
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
   * <p>Returns metadata fields.</p>
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
    return "CrudResponse{" +
        "response=" + response +
        ", error=" + error +
        '}';
  }
}

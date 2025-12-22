/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping.crud;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.tarantool.mapping.Field;

/**
 * <p>The class implements data that comes in response to CRUD operations from Tarantool.</p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see CrudError
 * @see <a href="https://github.com/tarantool/crud">crud</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrudData<T> {

  /**
   * <p>Space schema metadata.</p>
   */
  List<Field> metadata;

  /**
   * <p>Tuple rows coming from Tarantool.</p>
   */
  T rows;

  /**
   * <p>Creates a {@link CrudData} object.</p>
   */
  public CrudData() {}

  /**
   * <p>Returns value of metadata field.</p>
   *
   * @return {@link #metadata} value.
   */
  public List<Field> getMetadata() {
    return metadata;
  }

  /**
   * <p>Sets value of {@link #metadata} field.</p>
   *
   * @param metadata value of {@link #metadata} field
   */
  @JsonSetter("metadata")
  public void setMetadata(List<Field> metadata) {
    this.metadata = metadata;
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
   * <p>Sets value of {@link #rows} field.</p>
   *
   * @param rows value of {@link #rows} field
   */
  @JsonSetter("rows")
  public void setRows(T rows) {
    this.rows = rows;
  }

  @Override
  public String toString() {
    return "CrudData{" +
        "metadata=" + metadata +
        ", rows=" + rows +
        '}';
  }
}

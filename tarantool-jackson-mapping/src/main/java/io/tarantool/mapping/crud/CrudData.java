/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.crud;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.tarantool.mapping.Field;

/**
 * The class implements data that comes in response to CRUD operations from Tarantool.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see CrudError
 * @see <a href="https://github.com/tarantool/crud">crud</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrudData<T> {

  /** Space schema metadata. */
  List<Field> metadata;

  /** Tuple rows coming from Tarantool. */
  T rows;

  /** Creates a {@link CrudData} object. */
  public CrudData() {}

  /**
   * Returns value of metadata field.
   *
   * @return {@link #metadata} value.
   */
  public List<Field> getMetadata() {
    return metadata;
  }

  /**
   * Sets value of {@link #metadata} field.
   *
   * @param metadata value of {@link #metadata} field
   */
  @JsonSetter("metadata")
  public void setMetadata(List<Field> metadata) {
    this.metadata = metadata;
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
   * Sets value of {@link #rows} field.
   *
   * @param rows value of {@link #rows} field
   */
  @JsonSetter("rows")
  public void setRows(T rows) {
    this.rows = rows;
  }

  @Override
  public String toString() {
    return "CrudData{" + "metadata=" + metadata + ", rows=" + rows + '}';
  }
}

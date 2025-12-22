/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.util.List;
import java.util.Map;

public class TarantoolResponse<T> {

  protected StringBuilder stringBuilder;
  /**
   * Data selection result.
   */
  protected T data;

  /**
   * The formats contain schemas of every tuple in response. Key is formatId, value is list of format fields.
   */
  protected Map<Integer, List<Field>> formats;

  public TarantoolResponse(T data, Map<Integer, List<Field>> formats) {
    this.data = data;
    this.formats = formats;
  }

  /**
   * Gets data.
   *
   * @return the data
   */
  public T get() {
    return data;
  }

  /**
   * Gets the schema formats.
   *
   * @return the schema formats in the map representation
   */
  public Map<Integer, List<Field>> getFormats() {
    return formats;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("TarantoolResponse(data = ")
          .append(get())
          .append(", formats = ")
          .append(getFormats())
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}

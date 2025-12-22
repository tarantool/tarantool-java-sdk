/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Result of IPROTO_SELECT request.
 *
 * @param <T> the type parameter
 * @author Artyom Dubinin
 */
public class SelectResponse<T> extends TarantoolResponse<T> {

  /** The position contains a position descriptor of last selected tuple. */
  protected byte[] position;

  /**
   * Instantiates a new Select response.
   *
   * @param data the data
   * @param position the position
   * @param formats schema formats
   */
  public SelectResponse(T data, byte[] position, Map<Integer, List<Field>> formats) {
    super(data, formats);
    this.position = position;
  }

  /**
   * Position contains a position descriptor of last selected tuple.
   *
   * @return the byte array
   */
  public byte[] getPosition() {
    return position;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("SelectResponse(data = ")
          .append(get())
          .append(", position = ")
          .append(Arrays.toString(getPosition()))
          .append(", format = ")
          .append(getFormats())
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.util.List;

public class Tuple<T> {

  private StringBuilder stringBuilder;
  private T data;
  private Integer formatId;
  private List<Field> format;

  public Tuple(T data, Integer formatId) {
    this.data = data;
    this.formatId = formatId;
    this.format = null;
  }

  public Tuple(T data, Integer formatId, List<Field> format) {
    this.data = data;
    this.formatId = formatId;
    this.format = format;
  }

  public T get() {
    return data;
  }

  public Integer getFormatId() {
    return formatId;
  }

  public List<Field> getFormat() {
    return format;
  }

  public void setFormat(List<Field> format) {
    this.format = format;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("Tuple(formatId = ")
          .append(getFormatId())
          .append(", data = ")
          .append(data)
          .append(", format = ")
          .append(format)
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}

/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.schema;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * @author Artyom Dubinin
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class Index {

  int spaceId;
  int indexId;
  String name;
  String type;
  Map<String, Object> opts;
  List<Object> parts;

  public Index() {
  }

  public int getSpaceId() {
    return spaceId;
  }

  public Index withSpaceId(int spaceId) {
    this.spaceId = spaceId;
    return this;
  }

  public int getIndexId() {
    return indexId;
  }

  public Index withIndexId(int indexId) {
    this.indexId = indexId;
    return this;
  }

  public String getName() {
    return name;
  }

  public Index withName(String name) {
    this.name = name;
    return this;
  }

  public String getType() {
    return type;
  }

  public Index withType(String type) {
    this.type = type;
    return this;
  }

  public Map<String, Object> getOpts() {
    return opts;
  }

  public Index withOpts(Map<String, Object> opts) {
    this.opts = opts;
    return this;
  }

  public List<Object> getParts() {
    return parts;
  }

  public Index withParts(List<Object> parts) {
    this.parts = parts;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Index index = (Index) o;

    if (spaceId != index.spaceId) {
      return false;
    }
    if (indexId != index.indexId) {
      return false;
    }
    if (!Objects.equals(name, index.name)) {
      return false;
    }
    if (!Objects.equals(type, index.type)) {
      return false;
    }
    if (!Objects.equals(opts, index.opts)) {
      return false;
    }
    return Objects.equals(parts, index.parts);
  }

  @Override
  public int hashCode() {
    int result = spaceId;
    result = 31 * result + indexId;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (opts != null ? opts.hashCode() : 0);
    result = 31 * result + (parts != null ? parts.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Index{" +
        "spaceId=" + spaceId +
        ", indexId=" + indexId +
        ", name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", opts=" + opts +
        ", parts=" + parts +
        '}';
  }
}

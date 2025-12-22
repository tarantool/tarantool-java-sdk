/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.tarantool.mapping.Field;

/**
 * @author Artyom Dubinin
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class Space {

  int id;
  int owner;
  String name;
  String engine;
  int fieldCount;
  Map<String, Object> flags;
  List<Field> format;
  Map<String, Index> indexes;

  public Space() {
    this.indexes = new HashMap<>();
  }

  public int getId() {
    return id;
  }

  public Space setId(int id) {
    this.id = id;
    return this;
  }

  public int getOwner() {
    return owner;
  }

  public Space setOwner(int owner) {
    this.owner = owner;
    return this;
  }

  public String getName() {
    return name;
  }

  public Space setName(String name) {
    this.name = name;
    return this;
  }

  public String getEngine() {
    return engine;
  }

  public Space setEngine(String engine) {
    this.engine = engine;
    return this;
  }

  public int getFieldCount() {
    return fieldCount;
  }

  public Space setFieldCount(int fieldCount) {
    this.fieldCount = fieldCount;
    return this;
  }

  public Map<String, Object> getFlags() {
    return flags;
  }

  public Space setFlags(Map<String, Object> flags) {
    this.flags = flags;
    return this;
  }

  public List<Field> getFormat() {
    return format;
  }

  public Space setFormat(List<Field> format) {
    this.format = format;
    return this;
  }

  public Index getIndex(String name) {
    return indexes.get(name);
  }

  public Space addIndex(Index index) {
    this.indexes.put(index.getName(), index);
    return this;
  }

  public Map<String, Index> getIndexes() {
    return indexes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Space space = (Space) o;

    if (id != space.id) {
      return false;
    }
    if (owner != space.owner) {
      return false;
    }
    if (fieldCount != space.fieldCount) {
      return false;
    }
    if (!Objects.equals(name, space.name)) {
      return false;
    }
    if (!Objects.equals(engine, space.engine)) {
      return false;
    }
    if (!Objects.equals(flags, space.flags)) {
      return false;
    }
    if (!Objects.equals(format, space.format)) {
      return false;
    }
    return Objects.equals(indexes, space.indexes);
  }

  @Override
  public int hashCode() {
    int result = id;
    result = 31 * result + owner;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (engine != null ? engine.hashCode() : 0);
    result = 31 * result + fieldCount;
    result = 31 * result + (flags != null ? flags.hashCode() : 0);
    result = 31 * result + (format != null ? format.hashCode() : 0);
    result = 31 * result + (indexes != null ? indexes.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Space{"
        + "id="
        + id
        + ", owner="
        + owner
        + ", name='"
        + name
        + '\''
        + ", engine='"
        + engine
        + '\''
        + ", fieldCount="
        + fieldCount
        + ", flags="
        + flags
        + ", format="
        + format
        + ", indexes="
        + indexes
        + '}';
  }
}

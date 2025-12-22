/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * @author Artyom Dubinin
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Field {

  String name;
  String type;
  Boolean isNullable;
  String collation;
  Object constraint;
  Object foreignKey;

  public Field() {
  }

  public Field(String name, String type, Boolean isNullable, String collation, Object constraint, Object foreignKey) {
    this.name = name;
    this.type = type;
    this.isNullable = isNullable;
    this.collation = collation;
    this.constraint = constraint;
    this.foreignKey = foreignKey;
  }

  public String getName() {
    return name;
  }

  @JsonSetter("name")
  public Field setName(String name) {
    this.name = name;
    return this;
  }

  public String getType() {
    return type;
  }

  @JsonSetter("type")
  public Field setType(String type) {
    this.type = type;
    return this;
  }

  public Boolean isNullable() {
    if (isNullable == null) {
      return false;
    }
    return isNullable;
  }

  @JsonSetter("is_nullable")
  public Field setNullable(Boolean nullable) {
    isNullable = nullable;
    return this;
  }

  public String getCollation() {
    return collation;
  }

  @JsonSetter("collation")
  public Field setCollation(String collation) {
    this.collation = collation;
    return this;
  }

  public Object getConstraint() {
    return constraint;
  }

  @JsonSetter("constraint")
  public Field setConstraint(Object constraint) {
    this.constraint = constraint;
    return this;
  }

  public Object getForeignKey() {
    return foreignKey;
  }

  @JsonSetter("foreign_key")
  public Field setForeignKey(Object foreignKey) {
    this.foreignKey = foreignKey;
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

    Field field = (Field) o;

    if (!Objects.equals(name, field.name)) {
      return false;
    }
    if (!Objects.equals(type, field.type)) {
      return false;
    }
    if (!Objects.equals(isNullable, field.isNullable)) {
      return false;
    }
    if (!Objects.equals(collation, field.collation)) {
      return false;
    }
    if (!Objects.equals(constraint, field.constraint)) {
      return false;
    }
    return Objects.equals(foreignKey, field.foreignKey);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (isNullable != null ? isNullable.hashCode() : 0);
    result = 31 * result + (collation != null ? collation.hashCode() : 0);
    result = 31 * result + (constraint != null ? constraint.hashCode() : 0);
    result = 31 * result + (foreignKey != null ? foreignKey.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Field{" +
        "name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", isNullable=" + isNullable +
        ", collation='" + collation + '\'' +
        ", constraint=" + constraint +
        ", foreignKey=" + foreignKey +
        '}';
  }

}

/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.crud;

public enum ConditionOperator {
  EQ("=="),

  LESS("<"),

  LESS_EQ("<="),

  GREATER(">"),

  GREATER_EQ(">=");

  private final String operator;

  ConditionOperator(String operator) {
    this.operator = operator;
  }

  @Override
  public String toString() {
    return this.operator;
  }
}

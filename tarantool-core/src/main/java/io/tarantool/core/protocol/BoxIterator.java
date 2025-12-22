/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

public enum BoxIterator {
  EQ(0),
  REQ(1),
  ALL(2),
  LT(3),
  LE(4),
  GE(5),
  GT(6),
  BITS_ALL_SET(7),
  BITS_ANY_SET(8),
  BITS_ALL_NOT_SET(9),
  OVERLAPS(10),
  NEIGHBOR(11);

  private final int code;

  BoxIterator(int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }
}

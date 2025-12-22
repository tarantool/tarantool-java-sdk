/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core;

/**
 * @author Artyom Dubinin
 */
public final class Assert {

  /**
   * Asserts if the passed expression is {@code true}
   *
   * @param expression returns boolean
   * @param message exception message
   * @throws IllegalArgumentException if the assertion fails
   */
  public static void state(boolean expression, String message) throws IllegalArgumentException {
    if (!expression) {
      throw new IllegalArgumentException(message != null ? message : "");
    }
  }
}

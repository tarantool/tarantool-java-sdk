/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core;

import java.time.Duration;

public final class TimeoutsUtils {

  private TimeoutsUtils() {}

  /**
   * @see #checkTimeoutIsNotNegative(Duration, RuntimeException)
   */
  public static void checkTimeoutIsNotNegative(Duration timeout) {
    checkTimeoutIsNotNegative(timeout, null);
  }

  /**
   * Checks that the passed timeout duration is not negative ({@code timeout >= 0})
   *
   * @param timeout must be not null
   * @param exception custom exception
   */
  public static void checkTimeoutIsNotNegative(Duration timeout, RuntimeException exception) {
    if (timeout.isNegative()) {
      throwException(timeout, exception);
    }
  }

  /**
   * @see #checkTimeoutIsPositive(Duration, RuntimeException)
   */
  public static void checkTimeoutIsPositive(Duration timeout) {
    checkTimeoutIsPositive(timeout, null);
  }

  /**
   * Checks that the passed timeout duration is positive ({@code timeout > 0}).
   *
   * @param timeout must be not null
   * @param exception custom exception
   */
  public static void checkTimeoutIsPositive(Duration timeout, RuntimeException exception) {
    if (timeout.isZero() || timeout.isNegative()) {
      throwException(timeout, exception);
    }
  }

  private static void throwException(Duration timeout, RuntimeException exception) {
    if (exception == null) {
      throw new IllegalArgumentException(
          "Timeout must be greater than 0!. Current value is '" + timeout + "'");
    } else {
      throw exception;
    }
  }
}

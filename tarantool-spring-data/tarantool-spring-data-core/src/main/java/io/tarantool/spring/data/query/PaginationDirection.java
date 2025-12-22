/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data.query;

/**
 * An enumeration indicating the direction of movement across pages.
 *
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public enum PaginationDirection {

  /**
   * Move forward through pages (from first to last).
   */
  FORWARD(1),

  /**
   * Move backward through pages (from last to first).
   */
  BACKWARD(-1);


  /**
   * Multiplier that is used to indicate the direction of pagination in
   * {@link io.tarantool.client.crud.options.SelectOptions.Builder#withFirst(int)}.
   */
  private final int multiplier;

  PaginationDirection(int multiplier) {
    this.multiplier = multiplier;
  }

  public int getMultiplier() {
    return multiplier;
  }

  /**
   * Reverse pagination direction.
   *
   * @return reversed pagination direction.
   */
  public PaginationDirection reverse() {
    return this == FORWARD ? BACKWARD : FORWARD;
  }
}

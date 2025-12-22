/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

/**
 * Select after field must contain a tuple from which selection must continue or its position. That mode tells connector
 * how to use bytes in after field.
 *
 * @author Artyom Dubinin
 */
public enum SelectAfterMode {
  /**
   * Position of last selected tuple to start iteration after it.
   */
  POSITION,
  /**
   * Last selected tuple to start iteration after it.
   */
  TUPLE
}

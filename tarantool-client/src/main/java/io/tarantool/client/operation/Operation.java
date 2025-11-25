/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.operation;

/**
 * Base interface for all operators used in update operations.
 *
 * <p>Each operation should contain operator tag and field identifier (number of field or name).
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
public interface Operation {

  /**
   * Returns value of operator field.
   *
   * @return character value with operator symbol
   */
  char getOperator();

  /**
   * Returns name of field operand of update operator.
   *
   * @return name or index of field in tuple.
   */
  Object getFieldIdentifier();
}

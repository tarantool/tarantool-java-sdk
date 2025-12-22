/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.operation;

/**
 * <p>Base interface for all operators used in update operations.</p>
 * <p>Each operation should contain operator tag and field identifier (number of field or name). </p>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
public interface Operation {

  /**
   * <p>Returns value of operator field.</p>
   *
   * @return character value with operator symbol
   */
  char getOperator();

  /**
   * <p>Returns name of field operand of update operator.</p>
   *
   * @return name or index of field in tuple.
   */
  Object getFieldIdentifier();
}

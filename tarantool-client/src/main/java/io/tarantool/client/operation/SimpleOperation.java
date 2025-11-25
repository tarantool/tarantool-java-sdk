/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.operation;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * The class implements 2-argument operators for <a
 * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/update">update
 * request</a>. It can be used in box and crud clients.
 *
 * <p>Operators which can be expressed by this class:
 *
 * <ul>
 *   <li>{@code =} assignment
 *   <li>{@code +} addition / increment
 *   <li>{@code -} subtraction / decrement
 *   <li>{@code &} bitwise AND
 *   <li>{@code |} bitwise OR
 *   <li>{@code ^} bitwise XOR
 *   <li>{@code !} field insertion
 *   <li>{@code #} field deletion
 * </ul>
 *
 * <p>Example:
 *
 * <blockquote>
 *
 * <pre>{@code
 * TarantoolCrudSpace space = crudClient.space("spaceName");
 * space.insert(Arrays.asList(1, false, "Vanya")).get();
 *
 * // Updates tuple and returns tuple (id = 1, isMarried = true, name = "Ivan")
 * List<?> res = space
 *     .update(
 *         Collections.singletonList(1),
 *         Operations
 *             .create()
 *             .set("name", "Ivan")
 *             .set("is_married", true)
 *      )
 *      .join();
 * }</pre>
 *
 * </blockquote>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @see Operation
 * @see Operations
 * @see StringSpliceOperation
 * @see io.tarantool.client.box.TarantoolBoxClient
 * @see io.tarantool.client.box.TarantoolBoxSpace
 * @see io.tarantool.client.crud.TarantoolCrudClient
 * @see io.tarantool.client.crud.TarantoolCrudSpace
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class SimpleOperation implements Operation {

  /** Operator defining the update operation. */
  private final char operator;

  /**
   * A name or number of field which is an operand for update operation. Can be positive integer
   * (1..N), negative (-1, ...) or string with field name.
   */
  private final Object fieldIdentifier;

  /**
   * A first argument for update operator. Usually is a value for field {@link #fieldIdentifier}.
   */
  private final Object value;

  /**
   * Creates a {@link Operation} object with the given parameters.
   *
   * @param operator {@link #operator}
   * @param fieldName {@link #fieldIdentifier}
   * @param value {@link #value}
   * @param allowNullValue flag to allow null in {@link #value}
   * @throws IllegalArgumentException when {@code fieldName == null or value == null}
   * @see Operation
   */
  public SimpleOperation(char operator, String fieldName, Object value, boolean allowNullValue)
      throws IllegalArgumentException {
    if (fieldName == null || fieldName.isEmpty()) {
      throw new IllegalArgumentException("fieldName can't be null or empty string");
    }
    if (value == null && !allowNullValue) {
      throw new IllegalArgumentException("value can't be null, but it can be empty list");
    }
    this.operator = operator;
    this.fieldIdentifier = fieldName;
    this.value = value;
  }

  /**
   * Creates a {@link Operation} object with the given parameters.
   *
   * @param operator {@link #operator}
   * @param fieldIndex {@link #fieldIdentifier}
   * @param value {@link #value}
   * @param allowNullValue flag to allow null in {@link #value}
   * @throws IllegalArgumentException when {@code fieldIndex == 0 or value == null}
   * @see Operation
   */
  public SimpleOperation(char operator, int fieldIndex, Object value, boolean allowNullValue)
      throws IllegalArgumentException {
    if (fieldIndex == 0) {
      throw new IllegalArgumentException("fieldIndex can't be null or zero");
    }
    if (value == null && !allowNullValue) {
      throw new IllegalArgumentException("value can't be null, but it can be empty list");
    }
    this.operator = operator;
    this.fieldIdentifier = fieldIndex;
    this.value = value;
  }

  /**
   * Returns value of operator field.
   *
   * @return {@link #operator} value.
   */
  @Override
  public char getOperator() {
    return operator;
  }

  /**
   * Returns name of field operand of update operation.
   *
   * @return {@link #fieldIdentifier} value.
   */
  @Override
  public Object getFieldIdentifier() {
    return fieldIdentifier;
  }

  /**
   * Returns value of operand in update operation.
   *
   * @return {@link #value} value.
   */
  public Object getValue() {
    return value;
  }
}

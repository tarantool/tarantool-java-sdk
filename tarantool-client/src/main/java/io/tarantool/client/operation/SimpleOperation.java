/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.operation;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * <p> The class implements 2-argument operators for
 * <a href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/update">update request</a>.
 * It can be used in box and crud clients.</p>
 * <p>Operators which can be expressed by this class:</p>
 * <ul>
 *   <li>{@code =} assignment</li>
 *   <li>{@code +} addition / increment</li>
 *   <li>{@code -} subtraction / decrement</li>
 *   <li>{@code &} bitwise AND</li>
 *   <li>{@code |} bitwise OR</li>
 *   <li>{@code ^} bitwise XOR</li>
 *   <li>{@code !} field insertion</li>
 *   <li>{@code #} field deletion</li>
 * </ul>
 * <p>Example:</p>
 * <blockquote><pre>{@code
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
 * }</pre></blockquote>
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

  /**
   * <p>Operator defining the update operation.</p>
   */
  private final char operator;

  /**
   * <p>A name or number of field which is an operand for update operation.</p>
   * Can be positive integer (1..N), negative (-1, ...) or string with field name.
   */
  private final Object fieldIdentifier;

  /**
   * <p>A first argument for update operator. Usually is a value for field {@link #fieldIdentifier}.</p>
   */
  private final Object value;

  /**
   * <p>Creates a {@link Operation} object with the given parameters.</p>
   *
   * @param operator        {@link #operator}
   * @param fieldName       {@link #fieldIdentifier}
   * @param value           {@link #value}
   * @param allowNullValue  flag to allow null in {@link #value}
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
   * <p>Creates a {@link Operation} object with the given parameters.</p>
   *
   * @param operator        {@link #operator}
   * @param fieldIndex      {@link #fieldIdentifier}
   * @param value           {@link #value}
   * @param allowNullValue  flag to allow null in {@link #value}
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
   * <p>Returns value of operator field.</p>
   *
   * @return {@link #operator} value.
   */
  @Override
  public char getOperator() {
    return operator;
  }

  /**
   * <p>Returns name of field operand of update operation.</p>
   *
   * @return {@link #fieldIdentifier} value.
   */
  @Override
  public Object getFieldIdentifier() {
    return fieldIdentifier;
  }

  /**
   * <p>Returns value of operand in update operation.</p>
   *
   * @return {@link #value} value.
   */
  public Object getValue() {
    return value;
  }
}

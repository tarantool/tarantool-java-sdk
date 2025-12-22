/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.operation;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * <p>The class implements splice operator for
 * <a href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/update">update request</a>.</p>
 * <p>In common cases it should not be used directly, better use {@link Operations} class with helper methods.
 * <p>Example:</p>
 * <blockquote><pre>{@code
 * TarantoolCrudSpace space = crudClient.space("spaceName");
 * space.insert(Arrays.asList(1, false, "Vanya")).get();
 *
 * // Updates tuple and returns tuple (id = 1, isMarried = true, name = "Vano")
 * List<?> res = space.update(
 *     Collections.singletonList(1),
 *     Operations
 *         .create()
 *         .stringSplice("name", 4, 2, "o")
 *         .set("is_married", true)
 *     )
 * ).join();
 * }</pre></blockquote>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @see Operation
 * @see Operations
 * @see SimpleOperation
 * @see io.tarantool.client.box.TarantoolBoxClient
 * @see io.tarantool.client.box.TarantoolBoxSpace
 * @see io.tarantool.client.crud.TarantoolCrudClient
 * @see io.tarantool.client.crud.TarantoolCrudSpace
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class StringSpliceOperation implements Operation {

  /**
   * <p> Operator defining the update operator.</p>
   */
  private final char operator = ':';

  /**
   * <p>A name or number of field which is an operand for splice operator.</p>
   * Can be positive integer (1..N), negative (-1, ...) or string with field name.
   */
  private final Object fieldIdentifier;

  /**
   * <p>An offset where string value of field should be spliced.</p>
   */
  private final int start;

  /**
   * <p>A count of symbols which should be deleted from string value of field starting at position {@link #start}.
   */
  private final int len;

  /**
   * <p> A string that will be placed into string value of value at the position {@link #start}
   */
  private final String toInsert;

  /**
   * <p>
   * Creates a {@link Operation} instance with the given parameters for splice operator.
   * </p>
   *
   * @param fieldName {@link #fieldIdentifier}
   * @param start     {@link #start}
   * @param len       {@link #len}
   * @param toInsert  {@link #toInsert}
   * @throws IllegalArgumentException when fieldName is {@code null} or empty string, start is zero, toInsert is
   *                                  {@code null} or empty string
   * @see Operation
   * @see Operations
   */
  public StringSpliceOperation(
      String fieldName,
      int start,
      int len,
      String toInsert) throws IllegalArgumentException {
    if (fieldName == null || fieldName.equals("")) {
      throw new IllegalArgumentException("fieldName can't be null or empty string");
    }
    if (start == 0) {
      throw new IllegalArgumentException("start can't be zero");
    }
    if (toInsert == null) {
      throw new IllegalArgumentException("toInsert can't be null");
    }
    this.fieldIdentifier = fieldName;
    this.start = start;
    this.len = len;
    this.toInsert = toInsert;
  }

  /**
   * <p>
   * Creates a {@link Operation} instance with the given parameters for splice operator.
   * </p>
   *
   * @param fieldIndex {@link #fieldIdentifier}
   * @param start      {@link #start}
   * @param len        {@link #len}
   * @param toInsert   {@link #toInsert}
   * @throws IllegalArgumentException when fieldIndex is zero, start is zero, toInsert is {@code null} or empty string
   * @see Operation
   * @see Operations
   */
  public StringSpliceOperation(
      int fieldIndex,
      int start,
      int len,
      String toInsert) throws IllegalArgumentException {
    if (fieldIndex == 0) {
      throw new IllegalArgumentException("fieldIdentifier can't be null or zero");
    }
    if (start == 0) {
      throw new IllegalArgumentException("start can't be zero");
    }
    if (toInsert == null) {
      throw new IllegalArgumentException("toInsert can't be null");
    }
    this.fieldIdentifier = fieldIndex;
    this.start = start;
    this.len = len;
    this.toInsert = toInsert;
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
   * Getter for {@link #start}
   *
   * @return starting offset
   */
  public int getStart() {
    return start;
  }

  /**
   * Getter for {@link #len}
   *
   * @return count of symbols/bytes to remove from string value at offset {@link #start}
   */
  public int getLen() {
    return len;
  }

  /**
   * Getter for {@link #toInsert}
   *
   * @return string which will be placed into string value of field after offset {@link #start}
   */
  public String getToInsert() {
    return toInsert;
  }
}

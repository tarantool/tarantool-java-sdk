/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.operation;

import java.util.LinkedList;

/**
 * Helper class for creating and managing operators for <a
 * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/update">update
 * request</a>. It can be used in box and crud clients.
 *
 * <p>This class provides helper methods for fast operators creating. Supports the following
 * operators:
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
 *   <li>{@code :} string splice
 * </ul>
 *
 * <p>Chaining is supported and it allows to create set of operations with comfortable syntax.
 *
 * <p>Example:
 *
 * <blockquote>
 *
 * <pre>{@code
 * TarantoolBoxSpace space = boxClient.space("spaceName");
 * space.insert(Arrays.asList(1, false, "Artyom")).get();
 *
 * // Updates tuple and returns tuple (id = 1, isMarried = false, name = "Artemiy")
 * List<?> res = space
 *     .update(
 *         Collections.singletonList(1),
 *         Operations
 *             .create()
 *             .set("name", "Artemiy")
 *             .set("is_married", true)
 *     )
 *     .join();
 * }</pre>
 *
 * </blockquote>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
public final class Operations extends LinkedList<Operation> {

  /** Serial version identifier for this serializable class */
  private static final long serialVersionUID = -2208389705628662951L;

  /**
   * Fabric method for operations list creation.
   *
   * @return instance of {@link Operations}
   */
  public static Operations create() {
    return new Operations();
  }

  /**
   * Creates operator for assignment ({@code =}) by field name and adds it into operations list.
   *
   * @param fieldName name of field in tuple (should be the same as in Tarantool, not Java POJO)
   * @param value a new value of field
   * @return instance of {@link Operations}
   */
  public Operations set(String fieldName, Object value) {
    this.add(new SimpleOperation('=', fieldName, value, true));
    return this;
  }

  /**
   * Creates operator for assignment ({@code =}) by field index and adds it into operations list.
   *
   * @param fieldIndex index of field in tuple
   * @param value a new value of field
   * @return instance of {@link Operations}
   */
  public Operations set(Integer fieldIndex, Object value) {
    this.add(new SimpleOperation('=', fieldIndex, value, true));
    return this;
  }

  /**
   * Creates operator for addition ({@code +}) by field name and adds it into operations list.
   *
   * @param fieldName name of field in tuple (should be the same as in Tarantool, not Java POJO)
   * @param value a number to increment a current numeric value of field
   * @return instance of {@link Operations}
   */
  public Operations increment(String fieldName, Number value) {
    this.add(new SimpleOperation('+', fieldName, value, false));
    return this;
  }

  /**
   * Creates operator for addition ({@code +}) by field index and adds it into operations list.
   *
   * @param fieldIndex index of field in tuple
   * @param value a number to increment a current numeric value of field
   * @return instance of {@link Operations}
   */
  public Operations increment(Integer fieldIndex, Number value) {
    this.add(new SimpleOperation('+', fieldIndex, value, false));
    return this;
  }

  /**
   * Creates operator for subtraction ({@code -}) by field name and adds it into operations list.
   *
   * @param fieldName name of field in tuple (should be the same as in Tarantool, not Java POJO)
   * @param value a number to decrement a current numeric value of field
   * @return instance of {@link Operations}
   */
  public Operations decrement(String fieldName, Number value) {
    this.add(new SimpleOperation('-', fieldName, value, false));
    return this;
  }

  /**
   * Creates operator for subtraction ({@code -}) by field index and adds it into operations list.
   *
   * @param fieldIndex index of field in tuple
   * @param value a number to increment current numeric value of field
   * @return instance of {@link Operations}
   */
  public Operations decrement(Integer fieldIndex, Number value) {
    this.add(new SimpleOperation('-', fieldIndex, value, false));
    return this;
  }

  /**
   * Creates operator for bitwise AND operation ({@code &}) by field name and adds it into
   * operations list.
   *
   * @param fieldName name of field in tuple (should be the same as in Tarantool, not Java POJO)
   * @param value a numeric bitmask for bitwise AND
   * @return instance of {@link Operations}
   */
  public Operations bitAnd(String fieldName, Number value) {
    this.add(new SimpleOperation('&', fieldName, value, false));
    return this;
  }

  /**
   * Creates operator for bitwise AND operation ({@code &}) by field index and adds it into
   * operations list.
   *
   * @param fieldIndex index of field in tuple
   * @param value a numeric bitmask for bitwise AND
   * @return instance of {@link Operations}
   */
  public Operations bitAnd(Integer fieldIndex, Number value) {
    this.add(new SimpleOperation('&', fieldIndex, value, false));
    return this;
  }

  /**
   * Creates operator for bitwise OR operation ({@code |}) by field name and adds it into operations
   * list.
   *
   * @param fieldName name of field in tuple (should be the same as in Tarantool, not Java POJO)
   * @param value a numeric bitmask for bitwise OR
   * @return instance of {@link Operations}
   */
  public Operations bitOr(String fieldName, Number value) {
    this.add(new SimpleOperation('|', fieldName, value, false));
    return this;
  }

  /**
   * Creates operator for bitwise OR operation ({@code |}) by field index and adds it into
   * operations list.
   *
   * @param fieldIndex index of field in tuple
   * @param value a numeric bitmask for bitwise OR
   * @return instance of {@link Operations}
   */
  public Operations bitOr(Integer fieldIndex, Number value) {
    this.add(new SimpleOperation('|', fieldIndex, value, false));
    return this;
  }

  /**
   * Creates operator for bitwise XOR operation ({@code |}) by field name and adds it into
   * operations list.
   *
   * @param fieldName name of field in tuple (should be the same as in Tarantool, not Java POJO)
   * @param value a numeric bitmask for bitwise XOR
   * @return instance of {@link Operations}
   */
  public Operations bitXor(String fieldName, Number value) {
    this.add(new SimpleOperation('^', fieldName, value, false));
    return this;
  }

  /**
   * Creates operator for bitwise XOR operation ({@code |}) by field index and adds it into
   * operations list.
   *
   * @param fieldIndex index of field in tuple
   * @param value a numeric bitmask for bitwise XOR
   * @return instance of {@link Operations}
   */
  public Operations bitXor(Integer fieldIndex, Number value) {
    this.add(new SimpleOperation('^', fieldIndex, value, false));
    return this;
  }

  /**
   * Creates operator for adding field operation ({@code !}) at position of field and adds it into
   * operations list.
   *
   * @param fieldName name of field in tuple (should be the same as in Tarantool, not Java POJO)
   * @param value value to insert at position of field in tuple
   * @return instance of {@link Operations}
   */
  public Operations addItem(String fieldName, Object value) {
    this.add(new SimpleOperation('!', fieldName, value, false));
    return this;
  }

  /**
   * Creates operator for adding field operation ({@code !}) at position and adds it into operations
   * list.
   *
   * @param fieldIndex position to insert new tuple item
   * @param value value to insert at position of field in tuple
   * @return instance of {@link Operations}
   */
  public Operations addItem(Integer fieldIndex, Object value) {
    this.add(new SimpleOperation('!', fieldIndex, value, false));
    return this;
  }

  /**
   * Creates operator for removing fields operation ({@code #}) at position of field pointed by name
   * and adds it into operations list.
   *
   * @param fieldName name of field in tuple (should be the same as in Tarantool, not Java POJO)
   * @param count count of fields to remove from position of field pointed by fieldName argument
   * @return instance of {@link Operations}
   */
  public Operations delItems(String fieldName, Number count) {
    this.add(new SimpleOperation('#', fieldName, count, false));
    return this;
  }

  /**
   * Creates operator for removing fields operation ({@code #}) at position and adds it into
   * operations list.
   *
   * @param fieldIndex position to remove {@code count} fields from tuple
   * @param count count of fields to remove
   * @return instance of {@link Operations}
   */
  public Operations delItems(Integer fieldIndex, Number count) {
    this.add(new SimpleOperation('#', fieldIndex, count, false));
    return this;
  }

  /**
   * Creates operator for string manipulation with value of field and adds it into operations list.
   *
   * @param fieldName name of field in tuple (should be the same as in Tarantool, not Java POJO)
   * @param start offset in string value of field
   * @param len count of bytes to delete starting from offset
   * @param toInsert string to place into string value of field
   * @return instance of {@link Operations}
   */
  public Operations stringSplice(String fieldName, Integer start, Integer len, String toInsert) {
    this.add(new StringSpliceOperation(fieldName, start, len, toInsert));
    return this;
  }

  /**
   * Creates operator for string manipulation with value of field and adds it into operations list.
   *
   * @param fieldIndex index of field in tuple
   * @param start offset in string value of field
   * @param len count of bytes to delete starting from offset
   * @param toInsert string to place into string value of field
   * @return instance of {@link Operations}
   */
  public Operations stringSplice(Integer fieldIndex, Integer start, Integer len, String toInsert) {
    this.add(new StringSpliceOperation(fieldIndex, start, len, toInsert));
    return this;
  }
}

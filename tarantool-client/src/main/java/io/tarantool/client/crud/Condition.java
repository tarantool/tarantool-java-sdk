/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.crud;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * <p> The class implements <a href="https://github.com/tarantool/crud#select-conditions"> condition</a>
 * for the CRUD select operation.</p>
 * <p>Example:</p>
 * <blockquote><pre>{@code
 *  TarantoolCrudSpace space = crudClient.space("spaceName");
 *
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  space.insert(Arrays.asList(1, true, "Vanya")).get();
 *
 *  // Defines select operation condition
 *  Condition cond = Condition.builder()
 *                              .withOperator("=")
 *                              .withFieldIdentifier("name")
 *                              .withValue("Vanya")
 *                              .build();
 *  // Selects tuple with condition. Returns tuple (id = 1, isMarried = true, name = "Vanya")
 *  List<List<?>> res = space.select(cond);
 * }</pre></blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudSpace
 * @see TarantoolCrudClient
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class Condition {

  /**
   * <p> Operator defining the selection condition.</p>
   */
  private final String operator;

  /**
   * <p>A name of field which is an operand for comparison operation in select condition.</p>
   */
  private final String fieldIdentifier;
  /**
   * <p>The value of {@link #fieldIdentifier}.</p>
   */
  private final Object value;

  /**
   * <p>Creates a {@link Condition} object with the given parameters.</p>
   *
   * @param operator        {@link #operator}
   * @param fieldIdentifier {@link #fieldIdentifier}
   * @param value           {@link #value}
   * @throws IllegalArgumentException when {@code operator == null or fieldIdentifier == null or value == null}
   * @see Condition
   */
  public Condition(String operator, String fieldIdentifier, Object value) throws IllegalArgumentException {
    if (operator == null) {
      throw new IllegalArgumentException("operator can't be null");
    }

    if (fieldIdentifier == null) {
      throw new IllegalArgumentException("fieldIdentifier can't be null");
    }

    this.operator = operator;
    this.fieldIdentifier = fieldIdentifier;
    this.value = value;
  }

  /**
   * Same as {@link #Condition(String, String, Object)}.
   *
   * @param operator        {@link ConditionOperator}.
   * @param fieldIdentifier {@link #fieldIdentifier}
   * @param value           {@link #value}
   * @throws IllegalArgumentException when {@code operator == null or fieldIdentifier == null}
   * @see Condition
   */
  public Condition(ConditionOperator operator, String fieldIdentifier, Object value) throws IllegalArgumentException {
    if (operator == null) {
      throw new IllegalArgumentException("operator can't be null");
    }

    if (fieldIdentifier == null) {
      throw new IllegalArgumentException("fieldIdentifier can't be null");
    }

    this.operator = operator.toString();
    this.fieldIdentifier = fieldIdentifier;
    this.value = value;
  }

  /**
   * <p>Creates new builder instance of this class.</p>
   *
   * @return {@link Condition.Builder} object
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * <p>Factory method. Creates a {@link Condition} object with the given options.</p>
   *
   * @param operator        {@link #operator}
   * @param fieldIdentifier {@link #fieldIdentifier}
   * @param value           {@link #value}
   * @return {@link Condition} object
   * @throws IllegalArgumentException when {@code operator == null or fieldIdentifier == null or value == null}
   */
  public static Condition create(String operator, String fieldIdentifier, Object value)
      throws IllegalArgumentException {
    return new Condition(operator, fieldIdentifier, value);
  }

  /**
   * Same as {@link #create(String, String, Object)}.
   *
   * @param operator        {@link ConditionOperator}
   * @param fieldIdentifier {@link #fieldIdentifier}
   * @param value           {@link #value}
   * @return {@link Condition} object
   * @throws IllegalArgumentException when {@code operator == null or fieldIdentifier == null or value == null}
   */
  public static Condition create(ConditionOperator operator, String fieldIdentifier, Object value)
      throws IllegalArgumentException {
    return new Condition(operator, fieldIdentifier, value);
  }

  /**
   * <p>Returns value of operator field.</p>
   *
   * @return {@link #operator} value.
   */
  public String getOperator() {
    return operator;
  }

  /**
   * <p>Returns identifier of field operand of condition.</p>
   *
   * @return {@link #operator} value.
   */
  public String getFieldIdentifier() {
    return fieldIdentifier;
  }

  /**
   * <p>Returns value of operand in condition.</p>
   *
   * @return {@link #value} value.
   */
  public Object getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Condition condition = (Condition) o;
    return operator.equals(condition.operator) && fieldIdentifier.equals(condition.fieldIdentifier) &&
        Objects.equals(value, condition.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operator, fieldIdentifier, value);
  }

  /**
   * <p>Builder class for {@link Condition}.</p>
   *
   * @see Condition
   */
  public static class Builder {

    /**
     * <p>See also: {@link Condition#operator}.</p>
     */
    private String operator;

    /**
     * <p>See also: {@link Condition#fieldIdentifier}.</p>
     */
    private String fieldIdentifier;

    /**
     * <p>See also: {@link Condition#value}.</p>
     */
    private Object value;

    /**
     * <p>Sets value of {@link #operator} field. Operator parameter can't be null.</p>
     *
     * @param operator value of operator field.
     * @return {@link Condition.Builder} object.
     * @throws IllegalArgumentException when {@code operator == null}
     * @see Condition#operator
     * @see Condition
     */
    public Builder withOperator(String operator) throws IllegalArgumentException {
      if (operator == null) {
        throw new IllegalArgumentException("operator can't be null");
      }
      this.operator = operator;
      return this;
    }

    /**
     * Same as {@link #withOperator(String)}.
     *
     * @param operator {@link ConditionOperator}
     * @return {@link Condition.Builder} object.
     * @throws IllegalArgumentException when {@code operator == null}
     * @see Condition#operator
     * @see Condition
     */
    public Builder withOperator(ConditionOperator operator) throws IllegalArgumentException {
      if (operator == null) {
        throw new IllegalArgumentException("operator can't be null");
      }
      this.operator = operator.toString();
      return this;
    }

    /**
     * <p>Sets value of {@link #fieldIdentifier} field. FieldIdentifier parameter can't be null.</p>
     *
     * @param fieldIdentifier value of fieldIdentifier field.
     * @return {@link Condition.Builder} object.
     * @throws IllegalArgumentException when {@code fieldIdentifier == null}
     * @see Condition#fieldIdentifier
     * @see Condition
     */
    public Builder withFieldIdentifier(String fieldIdentifier) throws IllegalArgumentException {
      if (fieldIdentifier == null) {
        throw new IllegalArgumentException("fieldIdentifier can't be null");
      }
      this.fieldIdentifier = fieldIdentifier;
      return this;
    }

    /**
     * <p>Sets value of {@link #value} field. Value parameter can't be null, but it can be an empty list.</p>
     *
     * @param value value of value field.
     * @return {@link Condition.Builder} object.
     * @see Condition#value
     * @see Condition
     */
    public Builder withValue(Object value) {
      this.value = value;
      return this;
    }

    /**
     * <p>Builds object of {@link Condition} class.</p>
     *
     * @return {@link Condition} object
     * @throws IllegalArgumentException when {@code operator == null or fieldIdentifier == null or value == null}
     * @see Condition
     */
    public Condition build() throws IllegalArgumentException {
      return new Condition(operator, fieldIdentifier, value);
    }
  }

}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data.query;

import static io.tarantool.spring.data.Helper.assertNotNull;
import io.tarantool.client.crud.Condition;
import io.tarantool.client.crud.options.SelectOptions;

public class TarantoolCriteria {

  private final Conditions conditions;

  private final SelectOptions.Builder selectOptionsBuilder;

  public TarantoolCriteria() {
    this.conditions = new Conditions();
    this.selectOptionsBuilder = SelectOptions.builder();
  }

  public void addCondition(final Condition condition) {
    assertNotNull(condition, "condition must be not null");
    this.conditions.add(condition);
  }

  public void addCondition(final int index, final Condition condition) {
    assertNotNull(condition, "condition must be not null");
    this.conditions.add(index, condition);
  }

  /** Quickly resets all {@link TarantoolCriteria} settings. */
  public void clear() {
    this.conditions.clear();
    this.selectOptionsBuilder.withFirst(SelectOptions.DEFAULT_LIMIT);
  }

  /**
   * Sets the FIRST parameter if the {@code find<First/Top>By<EntityField>(...)} method is
   * specified.
   *
   * @param rows linit from method declaration
   */
  public void withFirst(final int rows) {
    this.selectOptionsBuilder.withFirst(rows);
  }

  public void withAfter(final Object tuple) {
    if (tuple != null) {
      this.selectOptionsBuilder.withAfter(tuple);
    }
  }

  /**
   * Builds and returns options for a select query.
   *
   * @return {@link SelectOptions}
   */
  public SelectOptions getOptions() {
    return this.selectOptionsBuilder.build();
  }

  /**
   * Returns the conditions for the resulting select query.
   *
   * @return {@link Conditions}
   */
  public Conditions getConditions() {
    return this.conditions;
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.crud;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.tarantool.client.operation.Operations;

/**
 * Structure representing single upsert operation. One upsert operation consists of two items: tuple
 * which will be inserted when space does not contain another tuple with the same key, and
 * operations as a second item. Operations is a list of field update operations applying to tuple
 * fields. It is the same as operations on update method.
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @see io.tarantool.client.operation.Operation
 * @see io.tarantool.client.operation.Operations
 * @see io.tarantool.client.crud.TarantoolCrudClient
 * @see io.tarantool.client.crud.TarantoolCrudSpace
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class UpsertData {

  /** Tuple which will be inserted into space if record with matching key does not exist. */
  private final Object tuple;

  /**
   * Update operations which will be applied to existing tuple in space. When tuple with the same
   * exist in space instead of replacing or trying to insert new tuple updating record will be
   * performed.
   *
   * @see io.tarantool.client.operation.Operation
   */
  private final Operations operations;

  /**
   * Constructor for this structure containing two fields: tuple and operations.
   *
   * @param tuple see {@link #tuple}
   * @param operations see {@link #operations}
   */
  public UpsertData(Object tuple, Operations operations) {
    this.tuple = tuple;
    this.operations = operations;
  }

  /**
   * @return the tuple
   */
  public Object getTuple() {
    return tuple;
  }

  /**
   * @return the operations
   */
  public Operations getOperations() {
    return operations;
  }
}

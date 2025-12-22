/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.crud;

import java.util.LinkedList;

import io.tarantool.client.operation.Operations;

/**
 * <p>This class represents a batch of {@link UpsertData} items being sent to upsertMany method.</p>
 * <p>Example:</p>
 * <blockquote><pre>{@code
 * TarantoolCrudSpace space = crudClient.space(spaceName);
 *
 * space.upsertMany(UpsertBatch.create()
 *     .add(Arrays.asList(1, false, "Ivan"),
 *          Operations.create()
 *                    .set("name", "Ivan")
 *                    .increment("age", 1))
 *     .add(Arrays.asList(2, true, "Maria"),
 *          Operations.create()
 *                    .set("name", "Masha")
 *                    .decrement("age", 1))).join();
 * }</pre></blockquote>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
public final class UpsertBatch extends LinkedList<UpsertData> {

  /**
   * Serialization version (needed by {@link java.io.Serializable})
   */
  private static final long serialVersionUID = 3029267810915657072L;

  /**
   * Factory method for fast creating batch instance. Just syntax sugar for {@code new UpsertBatch()}.
   *
   * @return batch instance of {@link UpsertBatch}
   */
  public static UpsertBatch create() {
    return new UpsertBatch();
  }

  /**
   * Appends UpsertData pair into list and returns the same instance batch, allowing chaining.
   *
   * @param tuple      a tuple to insert in case of missing key in space. See {@link UpsertData#tuple}.
   * @param operations a list of update operation applying to tuple with matching key in space.
   * @return the same instance of {@link UpsertBatch}
   */
  public UpsertBatch add(Object tuple, Operations operations) {
    this.add(new UpsertData(tuple, operations));
    return this;
  }
}

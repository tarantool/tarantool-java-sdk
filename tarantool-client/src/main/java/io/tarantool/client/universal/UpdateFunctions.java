/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.universal;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Defines the contract for update operations in a universal Tarantool client interface.
 *
 * <p>This interface provides methods for updating objects in Tarantool spaces using operation-based
 * updates, enabling abstract APIs that can work with various Tarantool client implementations
 * without being tied to specific details.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 */
public interface UpdateFunctions {

  /**
   * Updates an object in the Tarantool space using the specified operations.
   *
   * @param key the key of the object to be updated
   * @param operations the list of update operations to apply
   * @return CompletableFuture representing the asynchronous update operation
   */
  CompletableFuture<?> update(Object key, List<List<?>> operations);
}

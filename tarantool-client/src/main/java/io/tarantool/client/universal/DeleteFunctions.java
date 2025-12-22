/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.universal;

import java.util.concurrent.CompletableFuture;

/**
 * <p>Defines the contract for delete operations in a universal Tarantool client interface.</p>
 * <p>This interface provides methods for deleting objects from Tarantool spaces,
 * enabling abstract APIs that can work with various Tarantool client implementations
 * without being tied to specific details.</p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 */
public interface DeleteFunctions {

  /**
   * <p>Deletes an object from the Tarantool space by its key.</p>
   *
   * @param key the key of the object to be deleted
   * @return CompletableFuture representing the asynchronous delete operation
   */
  CompletableFuture<?> delete(Object key);
}

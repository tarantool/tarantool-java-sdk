/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.universal;

import java.util.concurrent.CompletableFuture;

/**
 * Defines the contract for delete operations in a universal Tarantool client interface.
 *
 * <p>This interface provides methods for deleting objects from Tarantool spaces, enabling abstract
 * APIs that can work with various Tarantool client implementations without being tied to specific
 * details.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 */
public interface DeleteFunctions {

  /**
   * Deletes an object from the Tarantool space by its key.
   *
   * @param key the key of the object to be deleted
   * @return CompletableFuture representing the asynchronous delete operation
   */
  CompletableFuture<?> delete(Object key);
}

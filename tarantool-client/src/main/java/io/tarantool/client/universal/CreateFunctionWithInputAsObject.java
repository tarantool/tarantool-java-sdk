/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.universal;

import java.util.concurrent.CompletableFuture;

/**
 * <p>Defines the contract for create operations with object input in a universal Tarantool client interface.</p>
 * <p>This interface provides methods for inserting and replacing objects in Tarantool spaces,
 * where the input is provided as a structured object containing field names and their corresponding values.
 * This abstraction enables building flexible APIs that can work with various Tarantool client implementations
 * without being tied to specific details or field structures.</p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 */
public interface CreateFunctionWithInputAsObject {

  /**
   * <p>Inserts an object into the Tarantool space.</p>
   * <p>The input object must contain field names and their corresponding values (unflattened structure),
   * which will be mapped to the appropriate fields in the Tarantool space.</p>
   *
   * @param object the structured object containing field names and values to be inserted into the space
   * @return CompletableFuture representing the asynchronous insert operation
   */
  CompletableFuture<?> insertObject(Object object);

  /**
   * <p>Replaces an object in the Tarantool space.</p>
   * <p>The input object must contain field names and their corresponding values (unflattened structure),
   * which will be mapped to the appropriate fields in the Tarantool space.</p>
   *
   * @param object the structured object containing field names and values to be replaced into the space
   * @return CompletableFuture representing the asynchronous replace operation
   */
  CompletableFuture<?> replaceObject(Object object);
}

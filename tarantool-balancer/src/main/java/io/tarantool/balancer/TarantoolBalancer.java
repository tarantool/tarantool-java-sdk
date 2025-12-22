/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.balancer;

import java.util.concurrent.CompletableFuture;

import io.tarantool.core.IProtoClient;
import io.tarantool.pool.IProtoClientPool;


public interface TarantoolBalancer {

  /**
   * <p>Default balancer class.</p>
   */
  Class<? extends TarantoolBalancer> DEFAULT_BALANCER_CLASS = TarantoolDistributingRoundRobinBalancer.class;

  /**
   * <p>Returns the next connection to execute the request. Method must be thread-safe</p>
   */
  CompletableFuture<IProtoClient> getNext();

  /**
   * <p>Returns the connection pool from which the connections will be returned by the method {@link #getNext()}</p>
   */
  IProtoClientPool getPool();

  /**
   * <p>Closes connection pool and other resources.</p>
   */
  void close() throws Exception;
}

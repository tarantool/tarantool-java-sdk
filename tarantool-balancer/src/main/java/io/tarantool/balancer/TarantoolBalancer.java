/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.balancer;

import java.util.concurrent.CompletableFuture;

import io.tarantool.core.IProtoClient;
import io.tarantool.pool.IProtoClientPool;

public interface TarantoolBalancer {

  /** Default balancer class. */
  Class<? extends TarantoolBalancer> DEFAULT_BALANCER_CLASS =
      TarantoolDistributingRoundRobinBalancer.class;

  /** Returns the next connection to execute the request. Method must be thread-safe */
  CompletableFuture<IProtoClient> getNext();

  /**
   * Returns the connection pool from which the connections will be returned by the method {@link
   * #getNext()}
   */
  IProtoClientPool getPool();

  /** Closes connection pool and other resources. */
  void close() throws Exception;
}

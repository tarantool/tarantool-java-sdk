/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client;

import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.universal.UniversalFunctions;
import io.tarantool.pool.IProtoClientPool;

/**
 * Implements a base contract for a space.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 */
public interface TarantoolSpace extends UniversalFunctions {

  /**
   * Gets instance of {@link TarantoolBalancer}.
   *
   * @return the balancer
   */
  TarantoolBalancer getBalancer();

  /**
   * Gets pool of {@link io.tarantool.core.IProtoClient}.
   *
   * @return the pool
   */
  IProtoClientPool getPool();
}

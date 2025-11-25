/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.Options;
import io.tarantool.client.TarantoolSpace;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.mapping.TarantoolJacksonMapping;
import io.tarantool.pool.IProtoClientPool;

abstract class IProtoCallClusterSpace extends AbstractTarantoolSpace implements TarantoolSpace {

  /**
   * The balancer used when sending requests.
   *
   * @see TarantoolBalancer
   */
  protected final TarantoolBalancer balancer;

  /** Space name. */
  protected final String spaceName;

  /**
   * This constructor creates {@link IProtoCallClusterSpace} based on the passed parameters.
   *
   * @param balancer see also: {@link #balancer}.
   * @param spaceName see also: {@link #spaceName}.
   */
  public IProtoCallClusterSpace(TarantoolBalancer balancer, String spaceName) {
    this.balancer = balancer;
    this.spaceName = spaceName;
  }

  /**
   * Converts arguments array to list of arguments by adding the space name to the beginning of the
   * list.
   *
   * @param arguments array of arguments.
   * @return list of key and options.
   */
  private List<Object> withSpaceName(Object[] arguments) {
    ArrayList<Object> args = new ArrayList<>(Arrays.asList(arguments));
    args.add(0, spaceName);
    return args;
  }

  /**
   * Sends a low-level call request based on the passed parameters.
   *
   * @param options {@link Options} object.
   * @param functionName crud function name.
   * @param args list of arguments.
   * @return if success - {@link CompletableFuture} with {@link IProtoResponse} object, otherwise -
   *     {@link CompletableFuture} with exception.
   */
  public CompletableFuture<IProtoResponse> iprotoCall(
      Options options, String functionName, Object... args) {
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }

    return balancer
        .getNext()
        .thenCompose(
            c ->
                c.call(
                    functionName,
                    TarantoolJacksonMapping.toValue(withSpaceName(args)),
                    null,
                    IProtoRequestOpts.empty()
                        .withRequestTimeout(options.getTimeout())
                        .withStreamId(options.getStreamId())));
  }

  @Override
  public TarantoolBalancer getBalancer() {
    return balancer;
  }

  @Override
  public IProtoClientPool getPool() {
    return balancer.getPool();
  }
}

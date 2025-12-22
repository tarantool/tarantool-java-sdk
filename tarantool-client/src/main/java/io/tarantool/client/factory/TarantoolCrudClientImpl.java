/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.util.Timer;

import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.client.crud.options.CrudOptions;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEventListener;
import io.tarantool.pool.TripleConsumer;

/**
 * <p>Class implementing {@link TarantoolClientImpl} and {@link TarantoolCrudClient}.</p>
 * <p>To use this class correctly, you can follow this example:</p>
 * <blockquote><pre>{@code
 *
 * // Creates crud client with default settings.
 * TarantoolCrudClient crudClient = TarantoolCrudClientImpl.builder().build();
 * ...
 *
 * TarantoolCrudSpace space = crudClient.space("spaceName");
 * ...
 *
 * }</pre></blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudSpace
 * @see CrudOptions
 * @see TarantoolBalancer
 * @see HeartbeatOpts
 */
final class TarantoolCrudClientImpl extends TarantoolClientImpl implements TarantoolCrudClient {

  /**
   * <p> This constructor creates {@link TarantoolCrudClientImpl} based on the passed parameters.</p>
   *
   * @param groups                see groups in{@link TarantoolCrudClientBuilder}.
   * @param channelOptions        see channelOptions in{@link TarantoolCrudClientBuilder}.
   * @param nThreads              see nThreads in{@link TarantoolCrudClientBuilder}.
   * @param timerResource         see timerService in{@link TarantoolCrudClientBuilder}.
   * @param gracefulShutdown      see gracefulShutdown in{@link TarantoolCrudClientBuilder}.
   * @param balancerClass         see balancerClass in{@link TarantoolCrudClientBuilder}.
   * @param heartbeatOpts         see heartbeatOpts in{@link TarantoolCrudClientBuilder}.
   * @param watcherOpts           see watcherOpts in{@link TarantoolCrudClientBuilder}.
   * @param connectTimeout        see connectTimeout in{@link TarantoolCrudClientBuilder}.
   * @param reconnectAfter        see reconnectAfter in{@link TarantoolCrudClientBuilder}.
   * @param metricsRegistry       see metricsRegistry in{@link TarantoolCrudClientBuilder}.
   * @param ignoredPacketsHandler see ignoredPacketsHandler in{@link TarantoolCrudClientBuilder}.
   * @param sslContext            see sslContext in{@link TarantoolCrudClientBuilder}.
   * @param useTupleExtension     see useTupleExtension in{@link TarantoolCrudClientBuilder}.
   * @throws NoSuchMethodException     if a matching method is not found.
   * @throws IllegalArgumentException  if the number of actual and formal parameters differ; if an unwrapping conversion
   *                                   for primitive arguments fails; or if, after possible unwrapping, a parameter
   *                                   value cannot be converted to the corresponding formal parameter type by a method
   *                                   invocation conversion; if this constructor pertains to an enum type.
   * @throws InstantiationException    if the class that declares the underlying constructor represents an abstract
   *                                   class.
   * @throws InvocationTargetException if the underlying constructor throws an exception.
   * @throws IllegalAccessException    if this {@code Constructor} object is enforcing Java language access control and
   *                                   the underlying constructor is inaccessible.
   */
  TarantoolCrudClientImpl(List<InstanceConnectionGroup> groups,
      Map<ChannelOption<?>, Object> channelOptions,
      int nThreads,
      ManagedResource<Timer> timerResource,
      boolean gracefulShutdown,
      Class<? extends TarantoolBalancer> balancerClass,
      HeartbeatOpts heartbeatOpts,
      WatcherOptions watcherOpts,
      long connectTimeout,
      long reconnectAfter,
      MeterRegistry metricsRegistry,
      TripleConsumer<String, Integer, IProtoResponse> ignoredPacketsHandler,
      SslContext sslContext,
      boolean useTupleExtension,
      PoolEventListener poolEventListener)
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
    super(groups,
        channelOptions,
        nThreads,
        timerResource,
        gracefulShutdown,
        balancerClass,
        heartbeatOpts,
        watcherOpts,
        connectTimeout,
        reconnectAfter,
        metricsRegistry,
        ignoredPacketsHandler,
        sslContext,
        useTupleExtension,
        poolEventListener);
  }

  /**
   * <p> Creates new builder for {@link TarantoolCrudClientImpl} class.</p>
   *
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public static TarantoolCrudClientBuilder builder() {
    return new TarantoolCrudClientBuilder();
  }

  @Override
  public TarantoolCrudSpaceImpl space(String name) {
    return new TarantoolCrudSpaceImpl(balancer, name);
  }
}

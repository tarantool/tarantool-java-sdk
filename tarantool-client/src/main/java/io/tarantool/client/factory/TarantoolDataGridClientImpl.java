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
import io.tarantool.client.tdg.TarantoolDataGridClient;
import io.tarantool.client.tdg.TarantoolDataGridSpace;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEventListener;
import io.tarantool.pool.TripleConsumer;

/**
 * <p>Class implementing {@link TarantoolClientImpl} and {@link TarantoolDataGridClient}.</p>
 * <p>To use this class correctly, you can follow this example:</p>
 * <blockquote><pre>{@code
 *
 * // Creates crud client with default settings.
 * TarantoolDataGridClient client = TarantoolDataGridClientImpl.builder().build();
 * ...
 *
 * TarantoolDataGridSpace space = client.space("spaceName");
 * ...
 *
 * }</pre></blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolDataGridSpace
 * @see TarantoolBalancer
 * @see HeartbeatOpts
 */
public class TarantoolDataGridClientImpl extends TarantoolClientImpl implements TarantoolDataGridClient {

  private final boolean useTdg1Context;
  private final Map<String, Object> credentials;

  protected TarantoolDataGridClientImpl(List<InstanceConnectionGroup> groups,
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
      boolean useTdg1Context,
      Map<String, Object> credentials,
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
    this.useTdg1Context = useTdg1Context;
    this.credentials = credentials;
    if (useTdg1Context) {
      this.eval(
          String.join(System.getProperty("line.separator"),
          "repository = {}",
          "repository.put = function(...)",
            "require('common.request_context').init({})",
            "local rep = require('input_processor.server').new().repository",
            "local data, err = rep:put(...)",
            "require('common.request_context').clear()",
            "return data, err",
          "end",
          "repository.find = function(...)",
            "require('common.request_context').init({})",
            "local rep = require('input_processor.server').new().repository",
            "local data, err = rep:find(...)",
            "require('common.request_context').clear()",
            "return data, err",
          "end",
          "repository.update = function(...)",
            "require('common.request_context').init({})",
            "local rep = require('input_processor.server').new().repository",
            "local data, err = rep:update(...)",
            "require('common.request_context').clear()",
            "return data, err",
          "end",
          "repository.delete = function(...)",
            "require('common.request_context').init({})",
            "local rep = require('input_processor.server').new().repository",
            "local data, err = rep:delete(...)",
            "require('common.request_context').clear()",
            "return data, err",
          "end"
          )).join();
    }
  }

/**
   * <p> Creates new builder for {@link TarantoolDataGridClientImpl} class.</p>
   *
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public static TarantoolDataGridClientBuilder builder() {
    return new TarantoolDataGridClientBuilder();
  }

  @Override
  public TarantoolDataGridSpaceImpl space(String name) {
    return new TarantoolDataGridSpaceImpl(balancer, name, useTdg1Context, credentials);
  }
}

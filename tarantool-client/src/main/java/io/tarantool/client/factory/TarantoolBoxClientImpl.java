/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.util.Timer;

import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.TarantoolVersion;
import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.box.TarantoolBoxSpace;
import io.tarantool.client.box.options.OptionsWithIndex;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.exceptions.ClientException;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEventListener;
import io.tarantool.pool.TripleConsumer;
import io.tarantool.schema.TarantoolSchemaFetcher;

/**
 * Class implementing {@link TarantoolClientImpl} and {@link TarantoolBoxClient}.
 *
 * <p>To use this class correctly, you can follow this example:
 *
 * <blockquote>
 *
 * <pre>{@code
 * // Creates box client with default settings.
 * TarantoolBoxClient boxClient = TarantoolBoxClientImpl.builder().build();
 * ...
 *
 * TarantoolBoxSpace space = boxClient.space("spaceName");
 * ...
 *
 * }</pre>
 *
 * </blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolBoxSpace
 * @see OptionsWithIndex
 * @see TarantoolSchemaFetcher
 * @see TarantoolBalancer
 * @see HeartbeatOpts
 */
final class TarantoolBoxClientImpl extends TarantoolClientImpl implements TarantoolBoxClient {

  /**
   * An object that fetches a new information about a schema only if version of schema is changed.
   *
   * @see TarantoolSchemaFetcher
   */
  private TarantoolSchemaFetcher fetcher;

  /**
   * This constructor creates {@link TarantoolBoxClientImpl} based on the passed parameters.
   *
   * @param groups {@link List} of connection groups. {@link InstanceConnectionGroup} is a list of N
   *     connections to one node.
   * @param channelOptions netty network channel settings.
   * @param nThreads number of threads provided by netty to serve connections.
   * @param timerResource timer that serves timeouts of requests sent to Tarantool. Ownership is
   *     encoded in the managed resource.
   * @param fetchSchema if {@code true}, then use {@link
   *     io.tarantool.schema.TarantoolSchemaFetcher}.
   * @param gracefulShutdown If {@code true}, then <a
   *     href="https://www.tarantool.io/en/doc/latest/dev_guide/internals/iproto/graceful_shutdown/">graceful
   *     shutdown</a> protocol is used.
   * @param balancerClass default type of {@link TarantoolBalancer} used in client.
   * @param heartbeatOpts if specified, heartbeat facility will be run with the passed {@link
   *     HeartbeatOpts options}.
   * @param watcherOpts if specified, watchers facility use passed {@link WatcherOptions options}.
   * @param connectTimeout connect timeout.
   * @param reconnectAfter time after which reconnect occurs.
   * @param metricsRegistry micrometer registry that hold set of collections of metrics.
   * @param ignoredPacketsHandler handler for ignored IProto-packets.
   * @param ignoreOldSchemaVersion if {@code false}, then client can raise exception on getting old
   *     schema version.
   * @param sslContext SslContext with settings for establishing SSL/TLS connection between
   *     Tarantool.
   * @throws NoSuchMethodException if a matching method is not found.
   * @throws IllegalArgumentException if the number of actual and formal parameters differ; if an
   *     unwrapping conversion for primitive arguments fails; or if, after possible unwrapping, a
   *     parameter value cannot be converted to the corresponding formal parameter type by a method
   *     invocation conversion; if this constructor pertains to an enum type.
   * @throws InstantiationException if the class that declares the underlying constructor represents
   *     an abstract class.
   * @throws InvocationTargetException if the underlying constructor throws an exception.
   * @throws IllegalAccessException if this {@code Constructor} object is enforcing Java language
   *     access control and the underlying constructor is inaccessible.
   */
  TarantoolBoxClientImpl(
      List<InstanceConnectionGroup> groups,
      Map<ChannelOption<?>, Object> channelOptions,
      int nThreads,
      ManagedResource<Timer> timerResource,
      boolean fetchSchema,
      boolean ignoreOldSchemaVersion,
      boolean gracefulShutdown,
      Class<? extends TarantoolBalancer> balancerClass,
      HeartbeatOpts heartbeatOpts,
      WatcherOptions watcherOpts,
      long connectTimeout,
      long reconnectAfter,
      MeterRegistry metricsRegistry,
      TripleConsumer<String, Integer, IProtoResponse> ignoredPacketsHandler,
      SslContext sslContext,
      PoolEventListener poolEventListener)
      throws InvocationTargetException,
          NoSuchMethodException,
          InstantiationException,
          IllegalAccessException {
    super(
        groups,
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
        !fetchSchema,
        poolEventListener);
    if (fetchSchema) {
      this.fetcher = new TarantoolSchemaFetcher(balancer, ignoreOldSchemaVersion);
    }
  }

  /**
   * Creates new builder for {@link TarantoolBoxClientImpl} class.
   *
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public static TarantoolBoxClientBuilder builder() {
    return new TarantoolBoxClientBuilder();
  }

  public TarantoolSchemaFetcher getFetcher() {
    return fetcher;
  }

  @Override
  public TarantoolBoxSpace space(int id) {
    return new TarantoolBoxSpaceImpl(balancer, id, fetcher);
  }

  @Override
  public TarantoolBoxSpace space(String name) {
    return new TarantoolBoxSpaceImpl(balancer, name, fetcher);
  }

  @Override
  public CompletableFuture<TarantoolVersion> getServerVersion() {
    return eval("return box.info.version")
        .thenApply(
            response -> {
              try {
                return TarantoolVersion.parse((String) response.get().get(0));
              } catch (Exception e) {
                throw new ClientException("Error parsing server version", e);
              }
            });
  }
}

/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.Timer;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_OK;
import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.Options;
import io.tarantool.client.TarantoolClient;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.core.exceptions.ServerException;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.mapping.TarantoolJacksonMapping;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.IProtoClientPool;
import io.tarantool.pool.IProtoClientPoolImpl;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEventListener;
import io.tarantool.pool.TripleConsumer;

/**
 * <p>Class implementing {@link TarantoolClient}.</p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
abstract class TarantoolClientImpl implements TarantoolClient {

  /**
   * <p>An object that regulates the load by balancing requests.</p>
   */
  protected final TarantoolBalancer balancer;
  /**
   * <p>Event loop groups for requests in Netty.</p>
   */
  private final MultiThreadIoEventLoopGroup nioEventLoopGroup;
  /**
   * Micrometer registry that hold set of collections of metrics.
   * <p>
   * See for details:
   * <a href="https://micrometer.io/docs/concepts#_registry">micrometer.io/docs/concepts#_registry</a>
   *
   * <p><i><b>Default</b></i>: {@code null}.</p>
   */
  private final MeterRegistry metricsRegistry;

  /**
   * <p>Client pool that is used in {@link #balancer}.</p>
   */
  private final IProtoClientPool pool;

  private final AtomicBoolean isClosed;

  /**
   * <p>Creates {@link TarantoolClientImpl} object with passed arguments.</p>
   *
   * @param groups                list of connection groups. {@link InstanceConnectionGroup} is a list of N connections
   *                              to one node. See: {@link InstanceConnectionGroup}
   * @param channelOptions        netty network channel settings
   * @param nThreads              number of threads provided by netty to serve connections
   * @param timerResource         timer that serves timeouts of requests sent to Tarantool. Ownership is encoded in the
   *                              managed resource.
   * @param gracefulShutdown      if {@code true}, then <a
   *                              href="https://www.tarantool.io/en/doc/latest/dev_guide/internals/iproto/graceful_shutdown/">graceful
   *                              shutdown</a> protocol is used
   * @param balancerClass         type of {@link TarantoolBalancer} used in client
   * @param heartbeatOpts         if specified, heartbeat facility will be run with the passed
   *                              {@link HeartbeatOpts options}
   * @param watcherOpts           if specified, watchers facility use passed {@link WatcherOptions options}
   * @param connectTimeout        connect timeout
   * @param reconnectAfter        time after which reconnect occurs
   * @param metricsRegistry       micrometer {@link TarantoolClientImpl#metricsRegistry}
   * @param ignoredPacketsHandler handler for ignored IProto-packets.
   * @param sslContext            SslContext with settings for establishing SSL/TLS connection between Tarantool.
   * @param useTupleExtension     Use TUPLE_EXT feature if true.
   * @param poolEventListener     listener that will receive pool lifecycle events.
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
  @SuppressWarnings("unchecked")
  protected TarantoolClientImpl(List<InstanceConnectionGroup> groups,
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
      throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    this.metricsRegistry = metricsRegistry;
    nioEventLoopGroup = new MultiThreadIoEventLoopGroup(nThreads, NioIoHandler.newFactory());
    Bootstrap bootstrap = new Bootstrap()
        .group(nioEventLoopGroup)
        .channel(NioSocketChannel.class);
    channelOptions.forEach((key, value) -> bootstrap.option((ChannelOption<Object>) key, value));
    ConnectionFactory factory = new ConnectionFactory(bootstrap, sslContext, timerResource.get());
    pool = new IProtoClientPoolImpl(
        factory,
        timerResource,
        gracefulShutdown,
        heartbeatOpts,
        watcherOpts,
        this.metricsRegistry,
        ignoredPacketsHandler,
        useTupleExtension,
        poolEventListener
    );
    pool.setGroups(groups);
    pool.setConnectTimeout(connectTimeout);
    pool.setReconnectAfter(reconnectAfter);

    Constructor<? extends TarantoolBalancer> constructor =
        balancerClass.getConstructor(IProtoClientPool.class);
    balancer = constructor.newInstance(pool);
    this.isClosed = new AtomicBoolean(false);
  }

  /**
   * <p>Method converts {@link Options} to {@link IProtoRequestOpts}.</p>
   *
   * @param opts {@link Options} object.
   * @return {@link IProtoRequestOpts} object.
   */
  private IProtoRequestOpts convertOptions(Options opts) {
    return IProtoRequestOpts.empty()
        .withRequestTimeout(opts.getTimeout())
        .withStreamId(opts.getStreamId());
  }

  /**
   * <p>Method converts {@link IProtoResponse responce} of "ping" request to boolean. If true - Tarantool is
   * available, otherwise - isn't available.</p>
   *
   * @param future {@link CompletableFuture} with {@link IProtoResponse}
   * @return {@link Boolean}. If true - Tarantool is available, otherwise - isn't available.
   */
  private CompletableFuture<Boolean> convertPingResult(CompletableFuture<IProtoResponse> future) {
    return future.thenApply((resp) -> resp.getRequestType() == IPROTO_OK);
  }

  @Override
  public CompletableFuture<TarantoolResponse<List<?>>> call(String function) {
    CompletableFuture<IProtoResponse> future =
        balancer.getNext().thenCompose(c -> c.call(function, ValueFactory.emptyArray()));

    return TarantoolJacksonMapping.convertFutureResult(future);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<List<T>>> call(String function, Class<T> entity) {
    CompletableFuture<IProtoResponse> future =
        this.balancer.getNext().thenCompose(c -> c.call(function, ValueFactory.emptyArray()));

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<T>> call(String function, TypeReference<T> entity) {
    CompletableFuture<IProtoResponse> future =
        balancer.getNext().thenCompose(c -> c.call(function, ValueFactory.emptyArray()));

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public CompletableFuture<TarantoolResponse<List<?>>> call(String function, List<?> args) {
    byte[] rawArgs = TarantoolJacksonMapping.toValue(args);
    CompletableFuture<IProtoResponse> future = balancer.getNext().thenCompose(c -> c.call(function, rawArgs));

    return TarantoolJacksonMapping.convertFutureResult(future);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<List<T>>> call(String function, List<?> args, Class<T> entity) {
    byte[] rawArgs = TarantoolJacksonMapping.toValue(args);
    CompletableFuture<IProtoResponse> future = balancer.getNext().thenCompose(c -> c.call(function, rawArgs));

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<T>> call(String function, List<?> args, TypeReference<T> entity) {
    byte[] rawArgs = TarantoolJacksonMapping.toValue(args);
    CompletableFuture<IProtoResponse> future = balancer.getNext().thenCompose(c -> c.call(function, rawArgs));

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public CompletableFuture<TarantoolResponse<List<?>>> call(String function, List<?> args, Options opts) {
    CompletableFuture<IProtoResponse> future =
        balancer.getNext().thenCompose(
            c -> c.call(
                function,
                TarantoolJacksonMapping.toValue(args),
                null,
                convertOptions(opts)
            )
        );

    return TarantoolJacksonMapping.convertFutureResult(future);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<List<T>>> call(String function, List<?> args, Options opts,
      Class<T> entity) {
    CompletableFuture<IProtoResponse> future = balancer.getNext().thenCompose(
        c -> c.call(
            function,
            TarantoolJacksonMapping.toValue(args),
            null,
            convertOptions(opts)
        )
    );

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<T>> call(String function, List<?> args, Object formats, Options opts,
      TypeReference<T> entity) {
    CompletableFuture<IProtoResponse> future = balancer.getNext().thenCompose(
        c -> c.call(
            function,
            TarantoolJacksonMapping.toValue(args),
            formats == null ? null : TarantoolJacksonMapping.toValueWithKeySerializer(formats),
            convertOptions(opts)
        )
    );

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public CompletableFuture<TarantoolResponse<List<?>>> eval(String expression) {
    CompletableFuture<IProtoResponse> future =
        balancer.getNext().thenCompose(c -> c.eval(expression, ValueFactory.emptyArray()));

    return TarantoolJacksonMapping.convertFutureResult(future);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<List<T>>> eval(String expression, Class<T> entity) {
    CompletableFuture<IProtoResponse> future =
        balancer.getNext().thenCompose(c -> c.eval(expression, ValueFactory.emptyArray()));

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<T>> eval(String expression, TypeReference<T> entity) {
    CompletableFuture<IProtoResponse> future =
        balancer.getNext().thenCompose(c -> c.eval(expression, ValueFactory.emptyArray()));

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public CompletableFuture<TarantoolResponse<List<?>>> eval(String expression, List<?> args) {
    CompletableFuture<IProtoResponse> future =
        balancer.getNext().thenCompose(c -> c.eval(expression, TarantoolJacksonMapping.toValue(args)));

    return TarantoolJacksonMapping.convertFutureResult(future);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<List<T>>> eval(String expression, List<?> args, Class<T> entity) {
    CompletableFuture<IProtoResponse> future =
        balancer.getNext().thenCompose(c -> c.eval(expression, TarantoolJacksonMapping.toValue(args)));

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<T>> eval(String expression, List<?> args, TypeReference<T> entity) {
    CompletableFuture<IProtoResponse> future =
        balancer.getNext().thenCompose(c -> c.eval(expression, TarantoolJacksonMapping.toValue(args)));

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public CompletableFuture<TarantoolResponse<List<?>>> eval(String expression, List<?> args, Options opts) {
    CompletableFuture<IProtoResponse> future = balancer.getNext().thenCompose(
        c -> c.eval(
            expression,
            TarantoolJacksonMapping.toValue(args),
            null,
            convertOptions(opts)
        )
    );

    return TarantoolJacksonMapping.convertFutureResult(future);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<List<T>>> eval(String expression, List<?> args, Options opts,
      Class<T> entity) {
    CompletableFuture<IProtoResponse> future = balancer.getNext().thenCompose(
        c -> c.eval(
            expression,
            TarantoolJacksonMapping.toValue(args),
            null,
            convertOptions(opts)
        )
    );

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<T>> eval(String expression, List<?> args, Object formats, Options opts,
      TypeReference<T> entity) {
    CompletableFuture<IProtoResponse> future = balancer.getNext().thenCompose(
        c -> c.eval(
            expression,
            TarantoolJacksonMapping.toValue(args),
            formats == null ? null : TarantoolJacksonMapping.toValueWithKeySerializer(formats),
            convertOptions(opts)
        )
    );

    return TarantoolJacksonMapping.convertFutureResult(future, entity);
  }

  @Override
  public CompletableFuture<Boolean> ping() {
    CompletableFuture<IProtoResponse> future = balancer.getNext().thenCompose(IProtoClient::ping);

    return convertPingResult(future);
  }

  @Override
  public CompletableFuture<Boolean> ping(Options opts) {
    CompletableFuture<IProtoResponse> future = balancer.getNext().thenCompose(c -> c.ping(convertOptions(opts)));

    return convertPingResult(future);
  }

  @Override
  public void watch(String key, Consumer<TarantoolResponse<?>> callback) {
    wrappedWatch(key, value -> callback.accept(TarantoolJacksonMapping.fromEventData(value)));
  }

  @Override
  public <T> void watch(String key, Consumer<TarantoolResponse<T>> callback, Class<T> entity) {
    wrappedWatch(key, value -> callback.accept(TarantoolJacksonMapping.fromEventData(value, entity)));
  }

  @Override
  public <T> void watch(String key, Consumer<TarantoolResponse<T>> callback, TypeReference<T> entity) {
    wrappedWatch(key, value -> callback.accept(TarantoolJacksonMapping.fromEventData(value, entity)));
  }

  private CompletableFuture<IProtoResponse> iprotoWatch(String key) {
    return balancer.getNext().thenCompose(
        c -> {
          Integer serverVersion = c.getServerProtocolVersion();
          if (serverVersion < 6) {
            throw new ServerException(
                "Tarantool doesn't support watch once feature. Need iproto version >= 6, got " + serverVersion
            );
          }
          return c.watchOnce(key);
        }
    );
  }

  @Override
  public CompletableFuture<TarantoolResponse<List<?>>> watchOnce(String key) {
    return TarantoolJacksonMapping.convertFutureResult(iprotoWatch(key));
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<List<T>>> watchOnce(String key, Class<T> entity) {
    return TarantoolJacksonMapping.convertFutureResult(iprotoWatch(key), entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<T>> watchOnce(String key, TypeReference<T> entity) {
    return TarantoolJacksonMapping.convertFutureResult(iprotoWatch(key), entity);
  }

  private void wrappedWatch(String key, Consumer<IProtoResponse> callback) {
    balancer.getPool().forEach(client -> client.watch(key, callback));
  }

  @Override
  public void unwatch(String key) {
    balancer.getPool().forEach(client -> client.unwatch(key));
  }

  @Override
  public void close() throws Exception {
    if (this.isClosed.compareAndSet(false, true)) {
      balancer.close();
      nioEventLoopGroup.shutdownGracefully().get();
    }
  }

  @Override
  public TarantoolBalancer getBalancer() {
    return balancer;
  }

  @Override
  public IProtoClientPool getPool() {
    return pool;
  }

  @Override
  public boolean isClosed() {
    return this.isClosed.get();
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.factory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import static io.tarantool.balancer.TarantoolBalancer.DEFAULT_BALANCER_CLASS;
import static io.tarantool.client.TarantoolClient.DEFAULT_CONNECTION_THREADS_NUMBER;
import static io.tarantool.client.TarantoolClient.DEFAULT_CONNECTION_TIMEOUT;
import static io.tarantool.client.TarantoolClient.DEFAULT_GRACEFUL_SHUTDOWN;
import static io.tarantool.client.TarantoolClient.DEFAULT_NETTY_CHANNEL_OPTIONS;
import static io.tarantool.client.TarantoolClient.DEFAULT_RECONNECT_AFTER;
import static io.tarantool.client.TarantoolClient.DEFAULT_TAG;
import static io.tarantool.client.crud.TarantoolCrudClient.DEFAULT_CRUD_PASSWORD;
import static io.tarantool.client.crud.TarantoolCrudClient.DEFAULT_CRUD_USERNAME;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_CONNECTION_NUMBER;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_HOST;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_PORT;
import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEventListener;
import io.tarantool.pool.TripleConsumer;

/** A specific builder for {@link TarantoolCrudClientImpl} class. */
public class TarantoolCrudClientBuilder {

  /** Default netty network channel settings. */
  private final Map<ChannelOption<?>, Object> options =
      new HashMap<>(DEFAULT_NETTY_CHANNEL_OPTIONS);

  /** Host name of Tarantool instance. */
  private String host = DEFAULT_HOST;

  /** Host port. */
  private int port = DEFAULT_PORT;

  /** Name of user which should be used for authorizing this connection. */
  private String user = DEFAULT_CRUD_USERNAME;

  /** Password for {@link #user}. */
  private String password = DEFAULT_CRUD_PASSWORD;

  /**
   * List of connection groups. {@link InstanceConnectionGroup} is a list of N connections to one
   * node.
   *
   * @see InstanceConnectionGroup
   */
  private List<InstanceConnectionGroup> groups;

  /**
   * Number of threads provided by netty to serve connections.
   *
   * <p><i><b>Default</b></i>: 0.
   *
   * @see MultiThreadIoEventLoopGroup
   */
  private int nThreads = DEFAULT_CONNECTION_THREADS_NUMBER;

  /** Timer that serves timeouts of requests sent to Tarantool. */
  private ManagedResource<Timer> timerResource;

  /**
   * If {@code true}, then <a
   * href="https://www.tarantool.io/en/doc/latest/dev_guide/internals/iproto/graceful_shutdown/">graceful
   * shutdown</a> protocol is used.
   *
   * <p><i><b>Default</b></i>: {@code true}.
   */
  private boolean gracefulShutdown = DEFAULT_GRACEFUL_SHUTDOWN;

  /**
   * If {@code true}, then tarantool will send tuples as extension values.
   *
   * <p><i><b>Default</b></i>: {@code false}.
   */
  private boolean useTupleExtension = false;

  /** Default type of {@link TarantoolBalancer} used in client. */
  private Class<? extends TarantoolBalancer> balancerClass = DEFAULT_BALANCER_CLASS;

  /**
   * If specified, heartbeat facility will be run with the passed {@link HeartbeatOpts options}.
   *
   * <p><i><b>Default</b></i>: {@code null}.
   */
  private HeartbeatOpts heartbeatOpts = null;

  /**
   * If specified, watchers facility use passed {@link WatcherOptions options}.
   *
   * <p><i><b>Default</b></i>: {@code null}.
   */
  private WatcherOptions watcherOpts = null;

  /**
   * Connect timeout.
   *
   * <p><i><b>Default</b></i>: {@code 3000L}.
   */
  private long connectTimeout = DEFAULT_CONNECTION_TIMEOUT;

  /**
   * Time after which reconnect occurs.
   *
   * <p><i><b>Default</b></i>: {@code 1000L}.
   */
  private long reconnectAfter = DEFAULT_RECONNECT_AFTER;

  /**
   * Micrometer registry that hold set of collections of metrics.
   *
   * <p>See for details: <a
   * href="https://micrometer.io/docs/concepts#_registry">micrometer.io/docs/concepts#_registry</a>
   *
   * <p><i><b>Default</b></i>: {@code null}.
   */
  private MeterRegistry metricsRegistry;

  /**
   * Handler for ignored IProto-packets.
   *
   * <p><i><b>Default</b></i>: {@code null}.
   */
  private TripleConsumer<String, Integer, IProtoResponse> ignoredPacketsHandler;

  /** SslContext with settings for establishing SSL/TLS connection between Tarantool */
  private SslContext sslContext;

  /** Optional listener for pool lifecycle events. */
  private PoolEventListener poolEventListener;

  public Map<ChannelOption<?>, Object> getOptions() {
    return options;
  }

  /**
   * Getter for {@link #host}.
   *
   * @return {@link String}.
   */
  public String getHost() {
    return host;
  }

  /**
   * Getter for {@link #port}.
   *
   * @return {@link Integer}.
   */
  public int getPort() {
    return port;
  }

  /**
   * Getter for {@link #user}.
   *
   * @return {@link String}.
   */
  public String getUser() {
    return user;
  }

  /**
   * Getter for {@link #password}.
   *
   * @return {@link String}.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Getter for {@link #groups}.
   *
   * @return {@link List} of {@link InstanceConnectionGroup}.
   */
  public List<InstanceConnectionGroup> getGroups() {
    return groups;
  }

  /**
   * Getter for {@link #nThreads}.
   *
   * @return {@link Integer}
   */
  public int getnThreads() {
    return nThreads;
  }

  /**
   * Getter for {@link #timerResource}.
   *
   * @return {@link Timer}.
   */
  public Timer getTimerService() {
    return timerResource != null ? timerResource.get() : null;
  }

  /**
   * Getter for {@link #gracefulShutdown}.
   *
   * @return {@link Boolean}.
   */
  public boolean isGracefulShutdown() {
    return gracefulShutdown;
  }

  /**
   * Getter for {@link #balancerClass}.
   *
   * @return {@link Class} of {@link io.tarantool.balancer.BalancerMode}.
   */
  public Class<? extends TarantoolBalancer> getBalancerClass() {
    return balancerClass;
  }

  /**
   * Getter for {@link #heartbeatOpts}.
   *
   * @return {@link HeartbeatOpts}.
   */
  public HeartbeatOpts getHeartbeatOpts() {
    return heartbeatOpts;
  }

  /**
   * Getter for {@link #watcherOpts}.
   *
   * @return {@link WatcherOptions}.
   */
  public WatcherOptions getWatcherOpts() {
    return watcherOpts;
  }

  /**
   * Getter for {@link #connectTimeout}.
   *
   * @return {@link Long}.
   */
  public long getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Getter for {@link #reconnectAfter}.
   *
   * @return {@link Long}.
   */
  public long getReconnectAfter() {
    return reconnectAfter;
  }

  /**
   * Getter for {@link #metricsRegistry}.
   *
   * @return {@link MeterRegistry}.
   */
  public MeterRegistry getMetricsRegistry() {
    return metricsRegistry;
  }

  /**
   * Getter for {@link #ignoredPacketsHandler}.
   *
   * @return {@link TripleConsumer}.
   */
  public TripleConsumer<String, Integer, IProtoResponse> getIgnoredPacketsHandler() {
    return ignoredPacketsHandler;
  }

  /**
   * Getter for {@link #sslContext}.
   *
   * @return {@link SslContext}.
   */
  public SslContext getSslContext() {
    return sslContext;
  }

  /**
   * Sets the {@link #groups} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #groups} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * InstanceConnectionGroup group = InstanceConnectionGroup.builder()
   *                                                        .withHost("hostName")
   *                                                        .withPort(port)
   *                                                        .withSize(connectionCount)
   *                                                        .withTag("tagName")
   *                                                        .withUser("userName")
   *                                                        .withPassword("password")
   *                                                        .build();
   *
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withGroups(Collections.singletonList(group))
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param groups see {@link #groups} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withGroups(List<InstanceConnectionGroup> groups) {
    this.groups = groups;
    return this;
  }

  /**
   * Sets the {@link #host} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #host} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withHost("localhost")
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param host see {@link #host} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withHost(String host) {
    if (host == null) {
      throw new IllegalArgumentException("Host can't be null");
    }
    this.host = host;
    return this;
  }

  /**
   * Sets the {@link #port} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #port} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withPort(3302)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param port see {@link #port} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * Sets the {@link #user} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #user} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withUser("userName")
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param user see {@link #user} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withUser(String user) {
    this.user = user;
    return this;
  }

  /**
   * Sets the {@link #password} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #password} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withPassword("password")
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param password see {@link #password} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * Sets the {@link #options} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #options} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withChannelOption(ChannelOption.TCP_NODELAY, false)
   *                                                  .withChannelOption(ChannelOption.SO_REUSEADDR, false)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param key see {@link ChannelOption} enum option.
   * @param value value for {@link ChannelOption} enum option.
   * @param <T> option type
   * @return {@link TarantoolCrudClientBuilder} object.
   * @throws IllegalArgumentException when {@code key == null or value == null}.
   */
  public <T> TarantoolCrudClientBuilder withChannelOption(ChannelOption<T> key, T value) {
    if (key == null) {
      throw new IllegalArgumentException("ChannelOption key can't be null");
    }
    options.put(key, value);
    return this;
  }

  /**
   * Similar to {@link #withChannelOption(ChannelOption, Object)}, but adds a map of options.
   *
   * @param channelOptions map of options to add.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withChannelOptions(
      Map<ChannelOption<?>, Object> channelOptions) {
    this.options.putAll(channelOptions);
    return this;
  }

  /**
   * Sets the {@link #nThreads} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #nThreads} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withNThreads(4)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param nThreads see {@link #nThreads} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withEventLoopThreadsCount(int nThreads) {
    this.nThreads = nThreads;
    return this;
  }

  /**
   * Sets the {@link #timerResource} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolCrudClientImpl} object with a specified timer
   * parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withTimerService(new HashedWheelTimer())
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param timerService see {@link #timerResource} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withTimerService(Timer timerService) {
    if (timerService == null) {
      throw new IllegalArgumentException("Timer can't be null");
    }
    this.timerResource = ManagedResource.external(timerService);
    return this;
  }

  /**
   * Sets the {@link #gracefulShutdown} parameter when constructing an instance of a builder class
   * to {@code false}. The following example creates a {@link TarantoolCrudClientImpl} object with
   * disabled {@link #gracefulShutdown} protocol:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .disableGracefulShutdown()
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder disableGracefulShutdown() {
    this.gracefulShutdown = false;
    return this;
  }

  /**
   * Sets the {@link #useTupleExtension} parameter when constructing an instance of a builder class
   * to {@code false}. The following example creates a {@link TarantoolCrudClientImpl} object with
   * enabled {@link #useTupleExtension} feature:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .enableTupleExtension()
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder enableTupleExtension() {
    this.useTupleExtension = true;
    return this;
  }

  /**
   * Sets the {@link #balancerClass} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #balancerClass} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withBalancerClass(TarantoolDistributingRoundRobinBalancer.class)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param balancerClass see {@link #balancerClass} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withBalancerClass(
      Class<? extends TarantoolBalancer> balancerClass) {
    if (balancerClass == null) {
      throw new IllegalArgumentException("BalancerClass key can't be null");
    }
    this.balancerClass = balancerClass;
    return this;
  }

  /**
   * Sets the {@link #heartbeatOpts} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #heartbeatOpts} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withHeartbeat(HeartbeatOpts.getDefault())
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param opts see {@link #heartbeatOpts} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withHeartbeat(HeartbeatOpts opts) {
    this.heartbeatOpts = opts;
    return this;
  }

  /**
   * Sets the {@link #watcherOpts} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #watcherOpts} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withWatcherOptions(WatcherOptions.builder().build())
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param opts see {@link #watcherOpts} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withWatcherOptions(WatcherOptions opts) {
    this.watcherOpts = opts;
    return this;
  }

  /**
   * Sets the {@link #connectTimeout} parameter when constructing an instance of a builder class.
   * The following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #connectTimeout} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withConnectTimeout(1_000L)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param timeout see {@link #connectTimeout} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withConnectTimeout(long timeout) {
    this.connectTimeout = timeout;
    return this;
  }

  /**
   * Sets the {@link #reconnectAfter} parameter when constructing an instance of a builder class.
   * The following example creates a {@link TarantoolCrudClientImpl} object with a specified {@link
   * #reconnectAfter} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *                                                  .withReconnectAfter(1_000L)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param after see {@link #reconnectAfter} field.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withReconnectAfter(long after) {
    this.reconnectAfter = after;
    return this;
  }

  /**
   * Set micrometer metricsRegistry. It can be used by {@link TarantoolFactory}.
   *
   * <p>See for details: <a
   * href="https://micrometer.io/docs/concepts#_registry">micrometer.io/docs/concepts#_registry</a>
   *
   * @param metricsRegistry micrometer metrics registry
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  TarantoolCrudClientBuilder withMeterRegistry(MeterRegistry metricsRegistry) {
    this.metricsRegistry = metricsRegistry;
    return this;
  }

  /**
   * Handler for processing packets.
   *
   * <p>This handler accepts tag, index of connection in pool, where packet was ignored and the
   * packet (instance of {@link io.tarantool.core.protocol.IProtoResponse}). For example it is
   * required to log all such packets to make analyse what is a problem with some connection from
   * some group.
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolCrudClient crudClient = TarantoolFactory.crud()
   *     .withIgnoredPacketsHandler((tag, index, packet) ->
   *         logger.warn(
   *             "ignored packet on connection %d from group %s, request id = %d, packet = %s",
   *             index,
   *             tag,
   *             packet
   *         )
   *     )
   *     .build();
   * }</pre>
   *
   * </blockquote>
   *
   * @param handler instance of handler.
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withIgnoredPacketsHandler(
      TripleConsumer<String, Integer, IProtoResponse> handler) {
    this.ignoredPacketsHandler = handler;
    return this;
  }

  /**
   * Specify SslContext with settings for establishing SSL/TLS connection between Tarantool
   *
   * @param sslContext {@link SslContext} instance
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withSslContext(SslContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  /**
   * Registers listener for Tarantool connection pool events.
   *
   * @param listener listener instance
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public TarantoolCrudClientBuilder withPoolEventListener(PoolEventListener listener) {
    this.poolEventListener = listener;
    return this;
  }

  /**
   * Builds specific {@link TarantoolCrudClient} class instance with parameters.
   *
   * @return {@link TarantoolCrudClient} object.
   * @throws Exception exception
   */
  public TarantoolCrudClient build() throws Exception {
    if (groups == null) {
      groups =
          Collections.singletonList(
              InstanceConnectionGroup.builder()
                  .withHost(host)
                  .withPort(port)
                  .withSize(DEFAULT_CONNECTION_NUMBER)
                  .withTag(DEFAULT_TAG)
                  .withUser(user)
                  .withPassword(password)
                  .build());
    }

    ManagedResource<Timer> actualTimerResource =
        timerResource == null
            ? ManagedResource.owned(new HashedWheelTimer(), Timer::stop)
            : timerResource;

    return new TarantoolCrudClientImpl(
        groups,
        options,
        nThreads,
        actualTimerResource,
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
}

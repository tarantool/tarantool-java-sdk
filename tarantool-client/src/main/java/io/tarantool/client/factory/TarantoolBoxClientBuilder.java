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
import static io.tarantool.client.box.TarantoolBoxClient.DEFAULT_BOX_USERNAME;
import static io.tarantool.client.box.TarantoolBoxClient.DEFAULT_FETCH_SCHEMA;
import static io.tarantool.client.box.TarantoolBoxClient.DEFAULT_IGNORE_OLD_SCHEMA_VERSION;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_CONNECTION_NUMBER;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_HOST;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_PORT;
import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEventListener;
import io.tarantool.pool.TripleConsumer;

/** A specific builder for {@link TarantoolBoxClientImpl} class. */
public class TarantoolBoxClientBuilder {

  /** Default netty network channel settings. */
  private final Map<ChannelOption<?>, Object> options =
      new HashMap<>(DEFAULT_NETTY_CHANNEL_OPTIONS);

  /** Host name of Tarantool instance. */
  private String host = DEFAULT_HOST;

  /** Host port. */
  private int port = DEFAULT_PORT;

  /** Name of user which should be used for authorizing this connection. */
  private String user = DEFAULT_BOX_USERNAME;

  /** Password for {@link #user}. */
  private String password = null;

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
   * If {@code true}, then use {@link io.tarantool.schema.TarantoolSchemaFetcher}.
   *
   * <p><i><b>Default</b></i>: {@code true}.
   */
  private boolean fetchSchema = DEFAULT_FETCH_SCHEMA;

  /**
   * If {@code false}, then client can raise exception on getting old schema version. Using it on
   * storage replicaset you need to ignore the errors because replicaset should have the same schema
   * version eventually. You have to use external schema fetcher if you want to use client for
   * different situation.
   *
   * <p><i><b>Default</b></i>: {@code true}.
   */
  private boolean ignoreOldSchemaVersion = DEFAULT_IGNORE_OLD_SCHEMA_VERSION;

  /**
   * If {@code true}, then <a
   * href="https://www.tarantool.io/en/doc/latest/dev_guide/internals/iproto/graceful_shutdown/">graceful
   * shutdown</a> protocol is used.
   *
   * <p><i><b>Default</b></i>: {@code true}.
   */
  private boolean gracefulShutdown = DEFAULT_GRACEFUL_SHUTDOWN;

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

  /** Optional listener for pool events. */
  private PoolEventListener poolEventListener;

  /**
   * Getter for {@link #options}.
   *
   * @return {@link Map}.
   */
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
   * @return {@link List}.
   */
  public List<InstanceConnectionGroup> getGroups() {
    return groups;
  }

  /**
   * Getter for {@link #nThreads}.
   *
   * @return {@link Integer}.
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
   * Getter for {@link #fetchSchema}.
   *
   * @return {@link Boolean}.
   */
  public boolean isFetchSchema() {
    return fetchSchema;
  }

  /**
   * Getter for {@link #ignoreOldSchemaVersion}.
   *
   * @return {@link Boolean}.
   */
  public boolean isIgnoreOldSchemaVersion() {
    return ignoreOldSchemaVersion;
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
   * @return {@link Class}.
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
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
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
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withGroups(Collections.singletonList(group))
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param groups see {@link #groups} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withGroups(List<InstanceConnectionGroup> groups) {
    this.groups = groups;
    return this;
  }

  /**
   * Sets the {@link #host} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #host} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withHost("localhost")
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param host see {@link #host} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withHost(String host) {
    if (host == null) {
      throw new IllegalArgumentException("Host can't be null");
    }
    this.host = host;
    return this;
  }

  /**
   * Sets the {@link #port} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #port} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withPort(3302)
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param port see {@link #port} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * Sets the {@link #user} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #user} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withUser("userName")
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param user see {@link #user} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withUser(String user) {
    this.user = user;
    return this;
  }

  /**
   * Sets the {@link #password} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #password} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withPassword("password")
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param password see {@link #password} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * Sets the {@link #options} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #options} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withChannelOption(ChannelOption.TCP_NODELAY, false)
   *                                                .withChannelOption(ChannelOption.SO_REUSEADDR, false)
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param key see {@link ChannelOption} enum option.
   * @param value value for {@link ChannelOption} enum option.
   * @param <T> return entity
   * @return {@link TarantoolBoxClient} object.
   * @throws IllegalArgumentException when {@code key == null or value == null}.
   */
  public <T> TarantoolBoxClientBuilder withChannelOption(ChannelOption<T> key, T value) {
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
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withChannelOptions(
      Map<ChannelOption<?>, Object> channelOptions) {
    this.options.putAll(channelOptions);
    return this;
  }

  /**
   * Sets the {@link #nThreads} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #nThreads} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withNThreads(4)
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param nThreads see {@link #nThreads} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withEventLoopThreadsCount(int nThreads) {
    this.nThreads = nThreads;
    return this;
  }

  /**
   * Sets the {@link #timerResource} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified timer
   * parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withTimerService(new HashedWheelTimer())
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param timerService see {@link #timerResource} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withTimerService(Timer timerService) {
    if (timerService == null) {
      throw new IllegalArgumentException("Timer can't be null");
    }
    this.timerResource = ManagedResource.external(timerService);
    return this;
  }

  /**
   * Sets the {@link #fetchSchema} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #fetchSchema} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withFetchSchema(false)
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param fetchSchema see {@link #fetchSchema} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withFetchSchema(boolean fetchSchema) {
    this.fetchSchema = fetchSchema;
    return this;
  }

  /**
   * Sets the {@link #ignoreOldSchemaVersion} parameter when constructing an instance of a builder
   * class to false. The following example creates a {@link TarantoolBoxClientImpl} that will raise
   * exception on getting packet with old schema version.
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .enableOldSchemaVersionCheck()
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder enableOldSchemaVersionCheck() {
    this.ignoreOldSchemaVersion = false;
    return this;
  }

  /**
   * Sets the {@link #gracefulShutdown} parameter when constructing an instance of a builder class
   * to {@code false}. The following example creates a {@link TarantoolBoxClientImpl} object with
   * disabled {@link #gracefulShutdown} protocol:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .disableGracefulShutdown()
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder disableGracefulShutdown() {
    this.gracefulShutdown = false;
    return this;
  }

  /**
   * Sets the {@link #balancerClass} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #balancerClass} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withBalancerClass(TarantoolDistributingRoundRobinBalancer.class)
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param balancerClass see {@link #balancerClass} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withBalancerClass(
      Class<? extends TarantoolBalancer> balancerClass) {
    if (balancerClass == null) {
      throw new IllegalArgumentException("BalancerClass key can't be null");
    }
    this.balancerClass = balancerClass;
    return this;
  }

  /**
   * Sets the {@link #heartbeatOpts} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #heartbeatOpts} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withHeartbeat(HeartbeatOpts.getDefault())
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param opts see {@link #heartbeatOpts} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withHeartbeat(HeartbeatOpts opts) {
    this.heartbeatOpts = opts;
    return this;
  }

  /**
   * Sets the {@link #watcherOpts} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #watcherOpts} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withWatcherOptions(WatcherOptions.builder().build())
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param opts see {@link #watcherOpts} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withWatcherOptions(WatcherOptions opts) {
    this.watcherOpts = opts;
    return this;
  }

  /**
   * Sets the {@link #connectTimeout} parameter when constructing an instance of a builder class.
   * The following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #connectTimeout} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withConnectTimeout(1_000L)
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param timeout see {@link #connectTimeout} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withConnectTimeout(long timeout) {
    this.connectTimeout = timeout;
    return this;
  }

  /**
   * Sets the {@link #reconnectAfter} parameter when constructing an instance of a builder class.
   * The following example creates a {@link TarantoolBoxClientImpl} object with a specified {@link
   * #reconnectAfter} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolBoxClient boxClient = TarantoolFactory.box()
   *                                                .withReconnectAfter(1_000L)
   *                                                .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param after see {@link #reconnectAfter} field.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withReconnectAfter(long after) {
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
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  TarantoolBoxClientBuilder withMeterRegistry(MeterRegistry metricsRegistry) {
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
   * TarantoolBoxClient boxClient =
   *      TarantoolFactory.box()
   *                      .withIgnoredPacketsHandler((tag, index, packet) -> {
   *                      logger.warn(
   *                              "ignored packet on connection %d from group %s, request id = %d, packet = %s",
   *                              index,
   *                              tag,
   *                              packet);
   *      }).build();
   * }</pre>
   *
   * </blockquote>
   *
   * @param handler instance of handler.
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withIgnoredPacketsHandler(
      TripleConsumer<String, Integer, IProtoResponse> handler) {
    this.ignoredPacketsHandler = handler;
    return this;
  }

  /**
   * Specify SslContext with settings for establishing SSL/TLS connection between Tarantool
   *
   * @param sslContext {@link SslContext} instance
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withSslContext(SslContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  /**
   * Registers listener for pool lifecycle events.
   *
   * @param listener listener instance
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public TarantoolBoxClientBuilder withPoolEventListener(PoolEventListener listener) {
    this.poolEventListener = listener;
    return this;
  }

  /**
   * Builds specific {@link TarantoolBoxClient} class instance with parameters.
   *
   * @return {@link TarantoolBoxClient} object.
   * @throws Exception exception
   */
  public TarantoolBoxClient build() throws Exception {
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

    return new TarantoolBoxClientImpl(
        groups,
        options,
        nThreads,
        actualTimerResource,
        fetchSchema,
        ignoreOldSchemaVersion,
        gracefulShutdown,
        balancerClass,
        heartbeatOpts,
        watcherOpts,
        connectTimeout,
        reconnectAfter,
        metricsRegistry,
        ignoredPacketsHandler,
        sslContext,
        poolEventListener);
  }
}

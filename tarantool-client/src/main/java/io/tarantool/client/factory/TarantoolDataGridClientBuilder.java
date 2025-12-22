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
import static io.tarantool.client.tdg.TarantoolDataGridClient.DEFAULT_TDG_PASSWORD;
import static io.tarantool.client.tdg.TarantoolDataGridClient.DEFAULT_TDG_USERNAME;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_CONNECTION_NUMBER;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_HOST;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_PORT;
import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.tdg.TarantoolDataGridClient;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEventListener;
import io.tarantool.pool.TripleConsumer;

/** A specific builder for {@link TarantoolDataGridClientImpl} class. */
public class TarantoolDataGridClientBuilder {

  /** Default netty network channel settings. */
  private final Map<ChannelOption<?>, Object> options =
      new HashMap<>(DEFAULT_NETTY_CHANNEL_OPTIONS);

  /** Host name of Tarantool instance. */
  private String host = DEFAULT_HOST;

  /** Host port. */
  private int port = DEFAULT_PORT;

  /** Name of user which should be used for authorizing this connection. */
  private String user = DEFAULT_TDG_USERNAME;

  /** Password for {@link #user}. */
  private String password = DEFAULT_TDG_PASSWORD;

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

  /**
   * If {@code true}, then tdg1 context will be used.
   *
   * <p><i><b>Default</b></i>: {@code false}.
   */
  private boolean useTdg1Context = false;

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

  /** Credentials for authentication */
  private Map<String, Object> credentials = Collections.emptyMap();

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
   * Getter for {@link #useTdg1Context}.
   *
   * @return {@link Boolean}.
   */
  public boolean isUseTdg1Context() {
    return useTdg1Context;
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
   * Getter for {@link #credentials}.
   *
   * @return {@link Map} of credentials.
   */
  public Map<String, Object> getCredentials() {
    return credentials;
  }

  /**
   * Sets the {@link #groups} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link
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
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withGroups(Collections.singletonList(group))
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param groups see {@link #groups} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withGroups(List<InstanceConnectionGroup> groups) {
    this.groups = groups;
    return this;
  }

  /**
   * Sets the {@link #host} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link
   * #host} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withHost("localhost")
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param host see {@link #host} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withHost(String host) {
    if (host == null) {
      throw new IllegalArgumentException("Host can't be null");
    }
    this.host = host;
    return this;
  }

  /**
   * Sets the {@link #port} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link
   * #port} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withPort(3302)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param port see {@link #port} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * Sets the {@link #user} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link
   * #user} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withUser("userName")
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param user see {@link #user} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withUser(String user) {
    this.user = user;
    return this;
  }

  /**
   * Sets the {@link #password} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link
   * #password} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withPassword("password")
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param password see {@link #password} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * Sets the {@link #options} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link
   * #options} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
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
   * @return {@link TarantoolDataGridClientBuilder} object.
   * @throws IllegalArgumentException when {@code key == null or value == null}.
   */
  public <T> TarantoolDataGridClientBuilder withChannelOption(ChannelOption<T> key, T value) {
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
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withChannelOptions(
      Map<ChannelOption<?>, Object> channelOptions) {
    this.options.putAll(channelOptions);
    return this;
  }

  /**
   * Sets the {@link #nThreads} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link
   * #nThreads} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withNThreads(4)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param nThreads see {@link #nThreads} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withEventLoopThreadsCount(int nThreads) {
    this.nThreads = nThreads;
    return this;
  }

  /**
   * Sets the {@link #timerResource} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with a specified timer
   * parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withTimerService(new HashedWheelTimer())
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param timerService see {@link #timerResource} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withTimerService(Timer timerService) {
    if (timerService == null) {
      throw new IllegalArgumentException("Timer can't be null");
    }
    this.timerResource = ManagedResource.external(timerService);
    return this;
  }

  /**
   * Sets the {@link #gracefulShutdown} parameter when constructing an instance of a builder class
   * to {@code false}. The following example creates a {@link TarantoolDataGridClientImpl} object
   * with disabled {@link #gracefulShutdown} protocol:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .disableGracefulShutdown()
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder disableGracefulShutdown() {
    this.gracefulShutdown = false;
    return this;
  }

  /**
   * Sets the {@link #useTupleExtension} parameter when constructing an instance of a builder class
   * to {@code false}. The following example creates a {@link TarantoolDataGridClientImpl} object
   * with enabled {@link #useTupleExtension} feature:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .enableTupleExtension()
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder enableTupleExtension() {
    this.useTupleExtension = true;
    return this;
  }

  /**
   * Sets the {@link #useTdg1Context} parameter when constructing an instance of a builder class to
   * {@code true}. The following example creates a {@link TarantoolDataGridClientImpl} object with
   * enabled {@link #useTdg1Context} feature:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .enableTdg1Context()
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder enableTdg1Context() {
    this.useTdg1Context = true;
    return this;
  }

  /**
   * Sets the {@link #balancerClass} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link
   * #balancerClass} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withBalancerClass(TarantoolDistributingRoundRobinBalancer.class)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param balancerClass see {@link #balancerClass} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withBalancerClass(
      Class<? extends TarantoolBalancer> balancerClass) {
    if (balancerClass == null) {
      throw new IllegalArgumentException("BalancerClass key can't be null");
    }
    this.balancerClass = balancerClass;
    return this;
  }

  /**
   * Sets the {@link #heartbeatOpts} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link
   * #heartbeatOpts} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withHeartbeat(HeartbeatOpts.getDefault())
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param opts see {@link #heartbeatOpts} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withHeartbeat(HeartbeatOpts opts) {
    this.heartbeatOpts = opts;
    return this;
  }

  /**
   * Sets the {@link #watcherOpts} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link
   * #watcherOpts} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withWatcherOptions(WatcherOptions.builder().build())
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param opts see {@link #watcherOpts} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withWatcherOptions(WatcherOptions opts) {
    this.watcherOpts = opts;
    return this;
  }

  /**
   * Sets the {@link #connectTimeout} parameter when constructing an instance of a builder class.
   * The following example creates a {@link TarantoolDataGridClientImpl} object with a specified
   * {@link #connectTimeout} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withConnectTimeout(1_000L)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param timeout see {@link #connectTimeout} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withConnectTimeout(long timeout) {
    this.connectTimeout = timeout;
    return this;
  }

  /**
   * Sets the {@link #reconnectAfter} parameter when constructing an instance of a builder class.
   * The following example creates a {@link TarantoolDataGridClientImpl} object with a specified
   * {@link #reconnectAfter} parameter:
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withReconnectAfter(1_000L)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param after see {@link #reconnectAfter} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withReconnectAfter(long after) {
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
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  TarantoolDataGridClientBuilder withMeterRegistry(MeterRegistry metricsRegistry) {
    this.metricsRegistry = metricsRegistry;
    return this;
  }

  /**
   * Handler for processing packets.
   *
   * <p>This handler accepts tag, index of connection in pool, where packet was ignored and the
   * packet (instance of {@link IProtoResponse}). For example it is required to log all such packets
   * to make analyse what is a problem with some connection from some group.
   *
   * <blockquote>
   *
   * <pre>{@code
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
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
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withIgnoredPacketsHandler(
      TripleConsumer<String, Integer, IProtoResponse> handler) {
    this.ignoredPacketsHandler = handler;
    return this;
  }

  /**
   * Specify SslContext with settings for establishing SSL/TLS connection between Tarantool
   *
   * @param sslContext {@link SslContext} instance
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withSslContext(SslContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  /**
   * Registers listener for connection pool lifecycle events.
   *
   * @param listener listener instance
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withPoolEventListener(PoolEventListener listener) {
    this.poolEventListener = listener;
    return this;
  }

  /**
   * Sets the {@link #credentials} parameter when constructing an instance of a builder class. The
   * following example creates a {@link TarantoolDataGridClientImpl} object with specified
   * credentials:
   *
   * <blockquote>
   *
   * <pre>{@code
   * Map<String, Object> credentials = new HashMap<>();
   * credentials.put("user", "username");
   * credentials.put("password", "password");
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withCredentials(credentials)
   *                                                  .build();
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @param credentials see {@link #credentials} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withCredentials(Map<String, Object> credentials) {
    this.credentials = credentials;
    return this;
  }

  /**
   * Builds specific {@link TarantoolDataGridClient} class instance with parameters.
   *
   * @return {@link TarantoolDataGridClient} object.
   * @throws Exception exception
   */
  public TarantoolDataGridClient build() throws Exception {
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

    return new TarantoolDataGridClientImpl(
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
        useTdg1Context,
        credentials,
        poolEventListener);
  }
}

/*
 * Copyright (c) 2025 VK Company Limited.
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
import io.tarantool.core.ManagedResource;

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
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEventListener;
import io.tarantool.pool.TripleConsumer;

/**
 * <p> A specific builder for {@link TarantoolDataGridClientImpl} class.</p>
 */
public class TarantoolDataGridClientBuilder {

  /**
   * <p>Default netty network channel settings.</p>
   */
  private final Map<ChannelOption<?>, Object> options = new HashMap<>(DEFAULT_NETTY_CHANNEL_OPTIONS);

  /**
   * <p>Host name of Tarantool instance.</p>
   */
  private String host = DEFAULT_HOST;

  /**
   * <p>Host port.</p>
   */
  private int port = DEFAULT_PORT;

  /**
   * <p>Name of user which should be used for authorizing this connection.</p>
   */
  private String user = DEFAULT_TDG_USERNAME;

  /**
   * <p>Password for {@link #user}.</p>
   */
  private String password = DEFAULT_TDG_PASSWORD;

  /**
   * <p>List of connection groups. {@link InstanceConnectionGroup} is a list of N
   * connections to one node.</p>
   *
   * @see InstanceConnectionGroup
   */
  private List<InstanceConnectionGroup> groups;

  /**
   * <p>Number of threads provided by netty to serve connections.</p>
   * <p><i><b>Default</b></i>: 0.</p>
   *
   * @see MultiThreadIoEventLoopGroup
   */
  private int nThreads = DEFAULT_CONNECTION_THREADS_NUMBER;

  /**
   * <p>Timer that serves timeouts of requests sent to Tarantool.</p>
   */
  private ManagedResource<Timer> timerResource;

  /**
   * <p>If {@code true}, then
   * <a href="https://www.tarantool.io/en/doc/latest/dev_guide/internals/iproto/graceful_shutdown/">graceful
   * shutdown</a> protocol is used.</p>
   * <p><i><b>Default</b></i>: {@code true}.</p>
   */
  private boolean gracefulShutdown = DEFAULT_GRACEFUL_SHUTDOWN;

  /**
   * <p>If {@code true}, then
   * tarantool will send tuples as extension values.
   * <p><i><b>Default</b></i>: {@code false}.</p>
   */
  private boolean useTupleExtension = false;

  /**
   * <p>If {@code true}, then
   * tdg1 context will be used.
   * <p><i><b>Default</b></i>: {@code false}.</p>
   */
  private boolean useTdg1Context = false;

  /**
   * <p>Default type of {@link TarantoolBalancer} used in client.</p>
   */
  private Class<? extends TarantoolBalancer> balancerClass = DEFAULT_BALANCER_CLASS;

  /**
   * <p>If specified, heartbeat facility will be run with the passed {@link HeartbeatOpts options}.</p>
   * <p><i><b>Default</b></i>: {@code null}.</p>
   */
  private HeartbeatOpts heartbeatOpts = null;

  /**
   * <p>If specified, watchers facility use passed {@link WatcherOptions options}.</p>
   * <p><i><b>Default</b></i>: {@code null}.</p>
   */
  private WatcherOptions watcherOpts = null;

  /**
   * <p>Connect timeout.</p>
   * <p><i><b>Default</b></i>: {@code 3000L}.</p>
   */
  private long connectTimeout = DEFAULT_CONNECTION_TIMEOUT;

  /**
   * <p>Time after which reconnect occurs.</p>
   * <p><i><b>Default</b></i>: {@code 1000L}.</p>
   */
  private long reconnectAfter = DEFAULT_RECONNECT_AFTER;

  /**
   * Micrometer registry that hold set of collections of metrics.
   * <p>
   * See for details:
   * <a href="https://micrometer.io/docs/concepts#_registry">micrometer.io/docs/concepts#_registry</a>
   *
   * <p><i><b>Default</b></i>: {@code null}.</p>
   */
  private MeterRegistry metricsRegistry;

  /**
   * <p>Handler for ignored IProto-packets.
   * <p><i><b>Default</b></i>: {@code null}.</p>
   */
  private TripleConsumer<String, Integer, IProtoResponse> ignoredPacketsHandler;

  /**
   * SslContext with settings for establishing SSL/TLS connection between Tarantool
   */
  private SslContext sslContext;

  /**
   * Credentials for authentication
   */
  private Map<String, Object> credentials = Collections.emptyMap();

  /**
   * Optional listener for pool lifecycle events.
   */
  private PoolEventListener poolEventListener;

  public Map<ChannelOption<?>, Object> getOptions() {
    return options;
  }

  /**
   * <p>Getter for {@link #host}.</p>
   *
   * @return {@link String}.
   */
  public String getHost() {
    return host;
  }

  /**
   * <p>Getter for {@link #port}.</p>
   *
   * @return {@link Integer}.
   */
  public int getPort() {
    return port;
  }

  /**
   * <p>Getter for {@link #user}.</p>
   *
   * @return {@link String}.
   */
  public String getUser() {
    return user;
  }

  /**
   * <p>Getter for {@link #password}.</p>
   *
   * @return {@link String}.
   */
  public String getPassword() {
    return password;
  }

  /**
   * <p>Getter for {@link #groups}.</p>
   *
   * @return {@link List} of {@link InstanceConnectionGroup}.
   */
  public List<InstanceConnectionGroup> getGroups() {
    return groups;
  }

  /**
   * <p>Getter for {@link #nThreads}.</p>
   *
   * @return {@link Integer}
   */
  public int getnThreads() {
    return nThreads;
  }

  /**
   * <p>Getter for {@link #timerResource}.</p>
   *
   * @return {@link Timer}.
   */
  public Timer getTimerService() {
    return timerResource != null ? timerResource.get() : null;
  }

  /**
   * <p>Getter for {@link #gracefulShutdown}.</p>
   *
   * @return {@link Boolean}.
   */
  public boolean isGracefulShutdown() {
    return gracefulShutdown;
  }

  /**
   * <p>Getter for {@link #useTdg1Context}.</p>
   *
   * @return {@link Boolean}.
   */
  public boolean isUseTdg1Context() {
    return useTdg1Context;
  }

  /**
   * <p>Getter for {@link #balancerClass}.</p>
   *
   * @return {@link Class} of {@link io.tarantool.balancer.BalancerMode}.
   */
  public Class<? extends TarantoolBalancer> getBalancerClass() {
    return balancerClass;
  }

  /**
   * <p>Getter for {@link #heartbeatOpts}.</p>
   *
   * @return {@link HeartbeatOpts}.
   */
  public HeartbeatOpts getHeartbeatOpts() {
    return heartbeatOpts;
  }

  /**
   * <p>Getter for {@link #watcherOpts}.</p>
   *
   * @return {@link WatcherOptions}.
   */
  public WatcherOptions getWatcherOpts() {
    return watcherOpts;
  }

  /**
   * <p>Getter for {@link #connectTimeout}.</p>
   *
   * @return {@link Long}.
   */
  public long getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * <p>Getter for {@link #reconnectAfter}.</p>
   *
   * @return {@link Long}.
   */
  public long getReconnectAfter() {
    return reconnectAfter;
  }

  /**
   * <p>Getter for {@link #metricsRegistry}.</p>
   *
   * @return {@link MeterRegistry}.
   */
  public MeterRegistry getMetricsRegistry() {
    return metricsRegistry;
  }

  /**
   * <p>Getter for {@link #ignoredPacketsHandler}.</p>
   *
   * @return {@link TripleConsumer}.
   */
  public TripleConsumer<String, Integer, IProtoResponse> getIgnoredPacketsHandler() {
    return ignoredPacketsHandler;
  }

  /**
   * <p>Getter for {@link #sslContext}.</p>
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
   * <p> Sets the {@link #groups} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link #groups}
   * parameter:
   * <blockquote><pre>{@code
   *
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
   * }</pre></blockquote>
   *
   * @param groups see {@link #groups} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withGroups(List<InstanceConnectionGroup> groups) {
    this.groups = groups;
    return this;
  }

  /**
   * <p> Sets the {@link #host} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link #host}
   * parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withHost("localhost")
   *                                                  .build();
   *
   * }</pre></blockquote>
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
   * <p> Sets the {@link #port} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link #port}
   * parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withPort(3302)
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @param port see {@link #port} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * <p> Sets the {@link #user} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link #user}
   * parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withUser("userName")
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @param user see {@link #user} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withUser(String user) {
    this.user = user;
    return this;
  }

  /**
   * <p> Sets the {@link #password} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link #password}
   * parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withPassword("password")
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @param password see {@link #password} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * <p> Sets the {@link #options} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link #options}
   * parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withChannelOption(ChannelOption.TCP_NODELAY, false)
   *                                                  .withChannelOption(ChannelOption.SO_REUSEADDR, false)
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @param key   see {@link ChannelOption} enum option.
   * @param value value for {@link ChannelOption} enum option.
   * @param <T>   option type
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
   * <p>Similar to {@link #withChannelOption(ChannelOption, Object)}, but adds a map of options.</p>
   *
   * @param channelOptions map of options to add.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withChannelOptions(Map<ChannelOption<?>, Object> channelOptions) {
    this.options.putAll(channelOptions);
    return this;
  }

  /**
   * <p> Sets the {@link #nThreads} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link #nThreads}
   * parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withNThreads(4)
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @param nThreads see {@link #nThreads} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withEventLoopThreadsCount(int nThreads) {
    this.nThreads = nThreads;
    return this;
  }

  /**
   * <p> Sets the {@link #timerResource} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified
   * timer parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withTimerService(new HashedWheelTimer())
   *                                                  .build();
   *
   * }</pre></blockquote>
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
   * <p> Sets the {@link #gracefulShutdown} parameter when constructing an instance of a builder
   * class to {@code false}. The following example creates a {@link TarantoolDataGridClientImpl} object with disabled
   * {@link #gracefulShutdown} protocol:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .disableGracefulShutdown()
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder disableGracefulShutdown() {
    this.gracefulShutdown = false;
    return this;
  }

  /**
   * <p> Sets the {@link #useTupleExtension} parameter when constructing an instance of a builder
   * class to {@code false}. The following example creates a {@link TarantoolDataGridClientImpl} object with enabled
   * {@link #useTupleExtension} feature:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .enableTupleExtension()
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder enableTupleExtension() {
    this.useTupleExtension = true;
    return this;
  }

  /**
   * <p> Sets the {@link #useTdg1Context} parameter when constructing an instance of a builder
   * class to {@code true}. The following example creates a {@link TarantoolDataGridClientImpl} object with enabled
   * {@link #useTdg1Context} feature:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .enableTdg1Context()
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder enableTdg1Context() {
    this.useTdg1Context = true;
    return this;
  }

  /**
   * <p> Sets the {@link #balancerClass} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified
   * {@link #balancerClass} parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withBalancerClass(TarantoolDistributingRoundRobinBalancer.class)
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @param balancerClass see {@link #balancerClass} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withBalancerClass(Class<? extends TarantoolBalancer> balancerClass) {
    if (balancerClass == null) {
      throw new IllegalArgumentException("BalancerClass key can't be null");
    }
    this.balancerClass = balancerClass;
    return this;
  }

  /**
   * <p> Sets the {@link #heartbeatOpts} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified
   * {@link #heartbeatOpts} parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withHeartbeat(HeartbeatOpts.getDefault())
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @param opts see {@link #heartbeatOpts} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withHeartbeat(HeartbeatOpts opts) {
    this.heartbeatOpts = opts;
    return this;
  }

  /**
   * <p> Sets the {@link #watcherOpts} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified {@link #watcherOpts}
   * parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withWatcherOptions(WatcherOptions.builder().build())
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @param opts see {@link #watcherOpts} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withWatcherOptions(WatcherOptions opts) {
    this.watcherOpts = opts;
    return this;
  }

  /**
   * <p> Sets the {@link #connectTimeout} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified
   * {@link #connectTimeout} parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withConnectTimeout(1_000L)
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @param timeout see {@link #connectTimeout} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withConnectTimeout(long timeout) {
    this.connectTimeout = timeout;
    return this;
  }

  /**
   * <p> Sets the {@link #reconnectAfter} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with a specified
   * {@link #reconnectAfter} parameter:
   * <blockquote><pre>{@code
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withReconnectAfter(1_000L)
   *                                                  .build();
   *
   * }</pre></blockquote>
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
   * <p>
   * See for details:
   * <a href="https://micrometer.io/docs/concepts#_registry">micrometer.io/docs/concepts#_registry</a>
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
   * <p>
   * This handler accepts tag, index of connection in pool, where packet was ignored and the packet (instance of
   * {@link IProtoResponse}). For example it is required to log all such packets to make
   * analyse what is a problem with some connection from some group.
   *
   * <blockquote><pre>{@code
   *
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
   * }</pre></blockquote>
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
   * Sets the {@link #credentials} parameter when constructing an instance of a builder
   * class. The following example creates a {@link TarantoolDataGridClientImpl} object with specified credentials:
   * <blockquote><pre>{@code
   *
   * Map<String, Object> credentials = new HashMap<>();
   * credentials.put("user", "username");
   * credentials.put("password", "password");
   *
   * TarantoolDataGridClient client = TarantoolFactory.tdg()
   *                                                  .withCredentials(credentials)
   *                                                  .build();
   *
   * }</pre></blockquote>
   *
   * @param credentials see {@link #credentials} field.
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public TarantoolDataGridClientBuilder withCredentials(Map<String, Object> credentials) {
    this.credentials = credentials;
    return this;
  }

  /**
   * <p> Builds specific {@link TarantoolDataGridClient} class instance with parameters.</p>
   *
   * @return {@link TarantoolDataGridClient} object.
   * @throws Exception exception
   */
  public TarantoolDataGridClient build() throws Exception {
    if (groups == null) {
      groups = Collections.singletonList(
          InstanceConnectionGroup.builder()
              .withHost(host)
              .withPort(port)
              .withSize(DEFAULT_CONNECTION_NUMBER)
              .withTag(DEFAULT_TAG)
              .withUser(user)
              .withPassword(password)
              .build());
    }

    ManagedResource<Timer> actualTimerResource = timerResource == null
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

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.exceptions.PoolClosedException;

/**
 * Basic pool implementation.
 *
 * <p>Example of usage:
 *
 * <blockquote>
 *
 * <pre>{@code
 * Bootstrap bootstrap = new Bootstrap()
 *     .group(new NioEventLoopGroup())
 *     .channel(NioSocketChannel.class)
 *     .option(ChannelOption.SO_REUSEADDR, true)
 *     .option(ChannelOption.SO_KEEPALIVE, true)
 *     .option(ChannelOption.TCP_NODELAY, true)
 *     .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
 * ManagedResource<Timer> timerService = ManagedResource.owned(new HashedWheelTimer(), Timer::stop);
 * ConnectionFactory factory = new ConnectionFactory(bootstrap, timerService.get());
 * IProtoClientPool pool = new IProtoClientPoolImpl(
 *     factory,
 *     timerService,
 *     true,
 *     HeartbeatOpts.getDefault(),
 *     WatcherOptions.builder().build()
 * );
 * pool.setGroups(Arrays.asList(
 *     InstanceConnectionGroup.builder()
 *                            .withHost("host-1")
 *                            .withPort(3301)
 *                            .withSize(10)
 *                            .withTag("node-1")
 *                            .build(),
 *     InstanceConnectionGroup.builder()
 *                            .withHost("host-2")
 *                            .withPort(3301)
 *                            .withSize(10)
 *                            .withTag("node-2")
 *                            .build()));
 * CompletableFuture<IProtoClient> futureClient = pool.get("node-2", 0);
 * }</pre>
 *
 * </blockquote>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
public class IProtoClientPoolImpl implements IProtoClientPool {

  /** Pool logger. */
  private static final Logger log = LoggerFactory.getLogger(IProtoClientPoolImpl.class);

  /** Bootstrap for connections. */
  private final ConnectionFactory factory;

  /** Map of pool entries, tag of group is key. */
  private final Map<String, List<PoolEntry>> entries;

  /** Map with {@link io.tarantool.pool.InstanceConnectionGroup} entries. */
  private final Map<String, InstanceConnectionGroup> groups;

  /** Service for handling timeouts. */
  private final ManagedResource<Timer> timerResource;

  /** Flag switching graceful shutdown handling. */
  private final boolean gracefulShutdown;

  /** Options for enabling and tuning heartbeats. */
  private final HeartbeatOpts heartbeatOpts;

  /** Options for tuning watchers in connections. */
  private final WatcherOptions watcherOpts;

  /** Count of connections which are invalidated or closed. */
  private final AtomicInteger unavailable;

  /** Count of connections which are reconnecting */
  private final AtomicInteger reconnecting;

  /** Metrics registry. */
  private final MeterRegistry metricsRegistry;

  /** Parameter that will say tarantool not to use Tuple Extension. */
  private final boolean useTupleExtension;

  /** Optional pool event listener. */
  private final PoolEventListener poolEventListener;

  /** Boolean flag denoting if pool closed or not. */
  private final AtomicBoolean isClosed;

  /** StringBuilder needed for fast string generation required for messages in exceptions. */
  private volatile StringBuilder stringBuilder;

  /** Connection timeout for clients in milliseconds. */
  private long connectTimeout;

  /** Time in milliseconds after reconnect will be run. */
  private long reconnectAfter;

  /** Total count of connections initialized in pool. */
  private int totalSize;

  /** Count of successful connection requests. */
  private Counter requestSuccess;

  /** Count of failed connection requests. */
  private Counter requestErrors;

  /** Count of connection requests for wrong tag or index. */
  private Counter invalidRequests;

  /**
   * Count of connection requests for connections which were invalidated or in reconnecting state.
   */
  private Counter lockedConnectionRequests;

  /**
   * Handler for accepting ignored packets and processing them somehow. It is an instance of {@link
   * io.tarantool.pool.TripleConsumer} which accepts three arguments: a first one is a tag of
   * connection, the second one is an index of connection in group and the third argument is a
   * packet.
   */
  private final TripleConsumer<String, Integer, IProtoResponse> ignoredPacketsHandler;

  private final Object connectionPoolLock = new Object();

  /**
   * Constructor for pool instance.
   *
   * <p>This creates s pool instance with some defaults:
   *
   * <ul>
   *   <li>gracefulShutdown = {@code true}
   *   <li>heartbeatOpts = {@code null}
   *   <li>watcherOpts = {@code null}
   * </ul>
   *
   * @param factory the bootstrap
   * @param timerResource managed timer resource (ownership is defined by the caller)
   */
  public IProtoClientPoolImpl(ConnectionFactory factory, ManagedResource<Timer> timerResource) {
    this(factory, timerResource, true, null, null, null, null, false, null);
  }

  /**
   * Constructor for pool instance.
   *
   * @param factory the bootstrap
   * @param timerService an instance of netty timer service (HashedWheelTimer). Ownership stays with
   *     caller.
   */
  public IProtoClientPoolImpl(ConnectionFactory factory, Timer timerService) {
    this(factory, ManagedResource.external(timerService));
  }

  /**
   * Constructor for pool instance.
   *
   * <p>This creates s pool instance with some defaults:
   *
   * <ul>
   *   <li>heartbeatOpts = {@code null}
   *   <li>watcherOpts = {@code null}
   * </ul>
   *
   * @param factory the bootstrap
   * @param timerResource managed timer resource (ownership is defined by the caller)
   * @param gracefulShutdown a boolean flag switching gracefulShutdown facility
   */
  public IProtoClientPoolImpl(
      ConnectionFactory factory, ManagedResource<Timer> timerResource, boolean gracefulShutdown) {
    this(factory, timerResource, gracefulShutdown, null, null, null, null, false, null);
  }

  /**
   * Constructor for pool instance.
   *
   * <p>This creates s pool instance with some defaults:
   *
   * <ul>
   *   <li>watcherOpts = {@code null}
   * </ul>
   *
   * @param factory the bootstrap
   * @param timerResource managed timer resource (ownership is defined by the caller)
   * @param gracefulShutdown a boolean flag switching gracefulShutdown facility
   * @param heartbeatOpts an object with options for heartbeats. If presented heartbeats will be
   *     used.
   */
  public IProtoClientPoolImpl(
      ConnectionFactory factory,
      ManagedResource<Timer> timerResource,
      boolean gracefulShutdown,
      HeartbeatOpts heartbeatOpts) {
    this(factory, timerResource, gracefulShutdown, heartbeatOpts, null, null, null, false, null);
  }

  /**
   * Constructor for pool instance.
   *
   * @param factory the bootstrap
   * @param timerResource managed timer resource (ownership is defined by the caller)
   * @param gracefulShutdown a boolean flag switching gracefulShutdown facility
   * @param heartbeatOpts an object with options for heartbeats. If presented heartbeats will be
   *     used.
   * @param watcherOpts an object with options for watchers
   * @param metricsRegistry an instance of MeterRegistry containing all necessary counters and
   *     gauges.
   */
  public IProtoClientPoolImpl(
      ConnectionFactory factory,
      ManagedResource<Timer> timerResource,
      boolean gracefulShutdown,
      HeartbeatOpts heartbeatOpts,
      WatcherOptions watcherOpts,
      MeterRegistry metricsRegistry) {
    this(
        factory,
        timerResource,
        gracefulShutdown,
        heartbeatOpts,
        watcherOpts,
        metricsRegistry,
        null,
        false,
        null);
  }

  /**
   * Constructor for pool instance.
   *
   * @param factory the bootstrap
   * @param timerResource managed timer resource (ownership is defined by the caller)
   * @param gracefulShutdown a boolean flag switching gracefulShutdown facility
   * @param heartbeatOpts an object with options for heartbeats. If presented heartbeats will be
   *     used.
   * @param watcherOpts an object with options for watchers
   * @param metricsRegistry an instance of MeterRegistry containing all necessary counters and
   *     gauges.
   * @param ignoredPacketsHandler a lambda for accepting ignored packets and handling them somehow.
   *     It is an instance of {@link io.tarantool.pool.TripleConsumer} which accepts three
   *     arguments: a first one is a tag of connection, the second one is an index of connection in
   *     group and the third argument is a packet.
   * @param useTupleExtension Use TUPLE_EXT feature if true.
   */
  public IProtoClientPoolImpl(
      ConnectionFactory factory,
      ManagedResource<Timer> timerResource,
      boolean gracefulShutdown,
      HeartbeatOpts heartbeatOpts,
      WatcherOptions watcherOpts,
      MeterRegistry metricsRegistry,
      TripleConsumer<String, Integer, IProtoResponse> ignoredPacketsHandler,
      boolean useTupleExtension,
      PoolEventListener poolEventListener) {
    this.factory = factory;
    this.entries = new ConcurrentHashMap<>();
    this.groups = new ConcurrentHashMap<>();
    this.timerResource = timerResource;
    this.connectTimeout = 3_000L; // TODO: https://github.com/tarantool/tarantool-java-ee/issues/412
    this.reconnectAfter = 1_000L;
    this.isClosed = new AtomicBoolean(false);
    this.gracefulShutdown = gracefulShutdown;
    this.heartbeatOpts = heartbeatOpts;
    this.unavailable = new AtomicInteger(0);
    this.reconnecting = new AtomicInteger(0);
    this.watcherOpts = watcherOpts;
    this.totalSize = 0;
    this.metricsRegistry = metricsRegistry;
    this.ignoredPacketsHandler = ignoredPacketsHandler;
    this.useTupleExtension = useTupleExtension;
    this.poolEventListener = poolEventListener;

    initMetrics();
  }

  @Override
  public void setGroups(List<InstanceConnectionGroup> clientGroups) {
    Map<String, Boolean> actualTags = new HashMap<>();
    int newTotal = 0;
    synchronized (connectionPoolLock) {
      for (InstanceConnectionGroup group : clientGroups) {
        String tag = group.getTag();
        int size = group.getSize();
        actualTags.put(tag, true);
        newTotal += size;

        InstanceConnectionGroup oldGroup = groups.put(tag, group);
        List<PoolEntry> connects = entries.computeIfAbsent(tag, k -> new ArrayList<>());
        if (oldGroup != null && !oldGroup.getAddress().equals(group.getAddress())) {
          shrinkGroup(connects, 0);
        }

        expandGroup(connects, group);
        shrinkGroup(connects, size);
      }
      for (String tag : groups.keySet()) {
        if (!actualTags.containsKey(tag)) {
          log.debug("Cleanup connections for old tag={}", tag);
          groups.remove(tag);
          shrinkGroup(entries.remove(tag), 0);
        }
      }
      totalSize = newTotal;
    }
  }

  @Override
  public List<String> getTags() {
    return new ArrayList<>(groups.keySet());
  }

  @Override
  public int getGroupSize(String tag) {
    InstanceConnectionGroup group = groups.get(tag);
    if (group == null) {
      synchronized (connectionPoolLock) {
        initStringBuilder();
        throw new NoSuchElementException(
            stringBuilder
                .delete(0, stringBuilder.length())
                .append("can't find connection group with tag ")
                .append(tag)
                .toString());
      }
    }
    return group.getSize();
  }

  @Override
  public CompletableFuture<IProtoClient> get(String tag, int index) {
    if (isClosed.get()) {
      incPoolInvalidRequests();
      throw new PoolClosedException("pool is closed");
    }

    synchronized (connectionPoolLock) {
      if (!groups.containsKey(tag)) {
        incPoolInvalidRequests();
        initStringBuilder();
        throw new NoSuchElementException(
            stringBuilder
                .delete(0, stringBuilder.length())
                .append("can't find connection group with tag ")
                .append(tag)
                .toString());
      }

      if (!entries.containsKey(tag)) {
        incPoolInvalidRequests();
        initStringBuilder();
        throw new NoSuchElementException(
            stringBuilder
                .delete(0, stringBuilder.length())
                .append("can't find connection entries with tag ")
                .append(tag)
                .toString());
      }

      try {
        PoolEntry entry = entries.get(tag).get(index);
        if (entry.isLocked()) {
          incPoolLockedConnectionRequests();
          return null;
        }
        CompletableFuture<IProtoClient> future = entry.connect();
        future.whenComplete(this::incPoolRequestCounters);
        return future;
      } catch (IndexOutOfBoundsException e) {
        incPoolInvalidRequests();
        initStringBuilder();
        throw new IndexOutOfBoundsException(
            stringBuilder
                .delete(0, stringBuilder.length())
                .append("group: ")
                .append(tag)
                .append(", index: ")
                .append(index)
                .toString());
      }
    }
  }

  @Override
  public void close() throws Exception {
    if (isClosed.compareAndSet(false, true)) {
      log.debug("Thread closes connection");
      synchronized (connectionPoolLock) {
        entries.forEach((tag, entryGroup) -> entryGroup.forEach(PoolEntry::close));
        entries.clear();
        groups.clear();
      }
      timerResource.close();
      log.info("close pool");
    }
  }

  @Override
  public long getConnectTimeout() {
    return connectTimeout;
  }

  @Override
  public void setConnectTimeout(long timeout) throws IllegalArgumentException {
    if (timeout <= 0) {
      throw new IllegalArgumentException("timeout should be positive number");
    }
    entries.forEach((tag, entryGroup) -> entryGroup.forEach(pe -> pe.setConnectTimeout(timeout)));
    connectTimeout = timeout;
  }

  @Override
  public boolean hasAvailableClients() {
    return unavailable.get() < totalSize;
  }

  @Override
  public int availableConnections() {
    return totalSize - unavailable.get();
  }

  @Override
  public long getReconnectAfter() {
    return reconnectAfter;
  }

  @Override
  public void setReconnectAfter(long reconnectAfter) throws IllegalArgumentException {
    if (reconnectAfter <= 0) {
      throw new IllegalArgumentException("reconnect period should be positive number");
    }
    this.reconnectAfter = reconnectAfter;
  }

  @Override
  public void forEach(Consumer<IProtoClient> action) {
    for (List<PoolEntry> group : entries.values()) {
      for (PoolEntry entry : group) {
        action.accept(entry.getClient());
      }
    }
  }

  /**
   * Expands a group of connects by creating and initializing new {@link
   * io.tarantool.pool.PoolEntry} instances.
   *
   * <p>This method is called when list of pool entries corresponding to tag and group has fewer
   * size than in new connections instance group configuration. This method creates new entries to
   * align this list with count of connections in instance group.
   *
   * @param connects list of {@link io.tarantool.pool.PoolEntry} entries
   * @param group an instance of {@link io.tarantool.pool.InstanceConnectionGroup}
   */
  private void expandGroup(List<PoolEntry> connects, InstanceConnectionGroup group) {
    log.info("create new connections: group {}", group.getTag());
    while (connects.size() < group.getSize()) {
      connects.add(
          new PoolEntry(
              factory,
              timerResource.get(),
              group,
              connects.size(),
              gracefulShutdown,
              connectTimeout,
              reconnectAfter,
              heartbeatOpts,
              watcherOpts,
              unavailable,
              reconnecting,
              metricsRegistry,
              ignoredPacketsHandler,
              useTupleExtension,
              poolEventListener));
    }
  }

  /**
   * Reduces an entries list to passed count.
   *
   * <p>This method is called when list of entries corresponding to tag and group has bigger size
   * than in new connections instance group configuration. This method deletes old clients to align
   * this list with count of connections in instance group.
   *
   * @param connects list of {@link io.tarantool.pool.PoolEntry} entries
   * @param count a new count of pool entries to reduce
   */
  private void shrinkGroup(List<PoolEntry> connects, int count) {
    if (connects == null || count >= connects.size()) {
      log.warn(
          "No connections to close. Opened connections:{}, keep open:{}",
          (connects != null) ? connects.size() : 0,
          count);
      return;
    }

    log.info("Closing {} connections", connects.size() - count);
    for (int i = connects.size() - 1; i >= count; i--) {
      PoolEntry entry = connects.remove(i);
      if (entry != null) {
        entry.unlock();
        entry.close();
      }
    }
  }

  /**
   * Used for initialization string builder used for generation exception messages.
   *
   * <p>Internal synchronized string builder is used for fast generation messages for exceptions
   * instead of using String.format() or string concatenation. It is quick and has a lower memory
   * usage.
   */
  private void initStringBuilder() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
    }
  }

  /** Metrics initialization. */
  private void initMetrics() {
    if (this.metricsRegistry == null) {
      return;
    }
    Gauge.builder("pool.size", () -> totalSize)
        .description("total number of connections at the moment")
        .register(metricsRegistry);
    Gauge.builder("pool.available", () -> totalSize - unavailable.get())
        .description("total number of connections available at the moment")
        .register(metricsRegistry);
    Gauge.builder("pool.invalidated", () -> unavailable.get() - reconnecting.get())
        .description("number of connections unavailable at the moment")
        .register(metricsRegistry);
    Gauge.builder("pool.reconnecting", reconnecting::get)
        .description("number of connections reconnecting at the moment")
        .register(metricsRegistry);

    Counter.builder("pool.connect.success")
        .description("count of succeeded connects")
        .register(metricsRegistry);
    Counter.builder("pool.connect.errors")
        .description("count of connect failures")
        .register(metricsRegistry);
    LongTaskTimer.builder("pool.connect.time")
        .description("time of connects")
        .register(metricsRegistry);

    requestSuccess =
        Counter.builder("pool.request.success")
            .description("Count of successful connections requests from pool")
            .register(metricsRegistry);
    requestErrors =
        Counter.builder("pool.request.errors")
            .description("Count of failed connection requests from pool")
            .register(metricsRegistry);
    invalidRequests =
        Counter.builder("pool.request.misses")
            .description("Count of connection requests with wrong tag or index")
            .register(metricsRegistry);
    lockedConnectionRequests =
        Counter.builder("pool.request.unavail")
            .description("Count of connection request finished with locked connect")
            .register(metricsRegistry);

    Counter.builder("pool.heartbeat.success")
        .description("Count of successful heartbeat ping requests")
        .register(metricsRegistry);
    Counter.builder("pool.heartbeat.errors")
        .description("Count of failed heartbeat ping requests")
        .register(metricsRegistry);
    LongTaskTimer.builder("pool.heartbeat.time")
        .description("Time of ping request")
        .register(metricsRegistry);
  }

  /**
   * Handler for {@link CompletableFuture#whenComplete} for incrementing counters depending on
   * result.
   *
   * @param r some result of future. Ignored.
   * @param e exception. When it is null then increments counter for success otherwise increments
   *     counter for errors.
   */
  private void incPoolRequestCounters(Object r, Throwable e) {
    if (requestSuccess == null || requestErrors == null) {
      return;
    }

    if (e == null) {
      requestSuccess.increment();
    } else {
      requestErrors.increment();
    }
  }

  /** Increment misses counter when wrong tag or index requested. */
  private void incPoolInvalidRequests() {
    if (invalidRequests == null) {
      return;
    }

    invalidRequests.increment();
  }

  /** Increment counter when invalidated or closed connection was requested. */
  private void incPoolLockedConnectionRequests() {
    if (lockedConnectionRequests == null) {
      return;
    }

    lockedConnectionRequests.increment();
  }

  @Override
  public ConnectionFactory getFactory() {
    return factory;
  }
}

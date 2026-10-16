/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
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
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.core.protocol.Handlers;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.PoolEntry.PoolEntryMetrics;
import io.tarantool.pool.exceptions.PoolClosedException;
import io.tarantool.pool.exceptions.PoolException;

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

  /** Statistics on unavailable and reconnecting and total connections. */
  private final ConnectionStatistics connectionStatistics;

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

  /** Metrics of {@link IProtoClientPoolImpl}. */
  private final PoolMetrics metrics;

  /**
   * Handler for accepting ignored packets and processing them somehow. It is an instance of {@link
   * io.tarantool.pool.TripleConsumer} which accepts three arguments: a first one is a tag of
   * connection, the second one is an index of connection in group and the third argument is a
   * packet.
   */
  private final TripleConsumer<String, Integer, IProtoResponse> ignoredPacketsHandler;

  private final Handlers handlers;

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
    this(factory, timerResource, true, null, null, null, null, null, false, null);
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
    this(factory, timerResource, gracefulShutdown, null, null, null, null, null, false, null);
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
    this(
        factory,
        timerResource,
        gracefulShutdown,
        heartbeatOpts,
        null,
        null,
        null,
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
   * @param poolEventListener optional pool event listener
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
    this(
        factory,
        timerResource,
        gracefulShutdown,
        heartbeatOpts,
        watcherOpts,
        metricsRegistry,
        ignoredPacketsHandler,
        null,
        useTupleExtension,
        poolEventListener);
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
   * @param handlers handlers for request/response lifecycle events
   * @param useTupleExtension Use TUPLE_EXT feature if true.
   * @param poolEventListener optional pool event listener
   */
  public IProtoClientPoolImpl(
      ConnectionFactory factory,
      ManagedResource<Timer> timerResource,
      boolean gracefulShutdown,
      HeartbeatOpts heartbeatOpts,
      WatcherOptions watcherOpts,
      MeterRegistry metricsRegistry,
      TripleConsumer<String, Integer, IProtoResponse> ignoredPacketsHandler,
      Handlers handlers,
      boolean useTupleExtension,
      PoolEventListener poolEventListener) {
    this.factory = factory;
    this.entries = new ConcurrentHashMap<>();
    this.groups = new ConcurrentHashMap<>();
    this.timerResource = timerResource;
    this.connectTimeout = 3_000L;
    this.reconnectAfter = 1_000L;
    this.isClosed = new AtomicBoolean(false);
    this.gracefulShutdown = gracefulShutdown;
    this.heartbeatOpts = heartbeatOpts;
    this.connectionStatistics = new ConnectionStatistics();
    this.watcherOpts = watcherOpts;
    this.metricsRegistry = metricsRegistry;
    this.ignoredPacketsHandler = ignoredPacketsHandler;
    this.handlers = handlers;
    this.useTupleExtension = useTupleExtension;
    this.poolEventListener = poolEventListener;

    this.metrics = initMetrics();
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
      this.connectionStatistics.setTotal(newTotal);
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
    return this.connectionStatistics.hasAvailableClients();
  }

  @Override
  public int availableConnections() {
    return this.connectionStatistics.availableConnections();
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
              this.connectionStatistics,
              metricsRegistry,
              ignoredPacketsHandler,
              handlers,
              useTupleExtension,
              poolEventListener,
              this.metrics != null ? this.metrics.poolEntryMetrics : null));
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
  private PoolMetrics initMetrics() {
    if (this.metricsRegistry == null) {
      return null;
    }

    return new PoolMetrics(metricsRegistry, this.connectionStatistics);
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
    if (this.metrics == null) {
      return;
    }

    if (e == null) {
      this.metrics.poolRequestSuccess.increment();
    } else {
      this.metrics.poolRequestErrors.increment();
    }
  }

  /** Increment misses counter when wrong tag or index requested. */
  private void incPoolInvalidRequests() {
    if (this.metrics != null) {
      this.metrics.poolRequestMisses.increment();
    }
  }

  /** Increment counter when invalidated or closed connection was requested. */
  private void incPoolLockedConnectionRequests() {
    if (this.metrics != null) {
      this.metrics.poolRequestUnavailable.increment();
    }
  }

  @Override
  public ConnectionFactory getFactory() {
    return factory;
  }

  /** Statistics on unavailable and reconnecting connections. Class is thread-safe. */
  static final class ConnectionStatistics {

    private final StampedLock lock = new StampedLock();

    /** Count of connections which are invalidated/closed + reconnecting. */
    private volatile int unavailable = 0;

    /** Count of connections which are reconnecting */
    private volatile int reconnecting = 0;

    /** Total count of connections initialized in pool. */
    private volatile int total = 0;

    int unavailable() {
      return this.unavailable;
    }

    int reconnecting() {
      return this.reconnecting;
    }

    public int availableConnections() {
      long stamp = lock.tryOptimisticRead();

      int localTotal = this.total;
      int localUnavailable = this.unavailable;

      if (!lock.validate(stamp)) {
        stamp = lock.readLock();
        try {
          localTotal = this.total;
          localUnavailable = this.unavailable;
        } finally {
          lock.unlockRead(stamp);
        }
      }

      final int totalAvailable = localTotal - localUnavailable;
      if (totalAvailable < 0) {
        throw new PoolException(
            String.format(
                "Invalid pool state! Pool has negative available connections! Total connections:"
                    + " %s, unavailable: %s, available: %s",
                localTotal, localUnavailable, totalAvailable)) {};
      }
      return totalAvailable;
    }

    public boolean hasAvailableClients() {
      long stamp = lock.tryOptimisticRead();

      int localTotal = this.total;
      int localUnavailable = this.unavailable;

      if (!lock.validate(stamp)) {
        stamp = lock.readLock();
        try {
          localTotal = this.total;
          localUnavailable = this.unavailable;
        } finally {
          lock.unlockRead(stamp);
        }
      }

      final int totalAvailable = localTotal - localUnavailable;
      if (totalAvailable < 0) {
        throw new PoolException(
            String.format(
                "Invalid pool state! Pool has negative available connections! Total connections:"
                    + " %s, unavailable: %s, available: %s",
                localTotal, localUnavailable, totalAvailable)) {};
      }
      return totalAvailable > 0;
    }

    public int invalidatedConnections() {
      long stamp = lock.tryOptimisticRead();

      int localUnavailable = this.unavailable;
      int localReconnecting = this.reconnecting;

      if (!lock.validate(stamp)) {
        stamp = lock.readLock();
        try {
          localUnavailable = this.unavailable;
          localReconnecting = this.reconnecting;
        } finally {
          lock.unlockRead(stamp);
        }
      }

      final int totalInvalidating = localUnavailable - localReconnecting;
      if (totalInvalidating < 0) {
        throw new PoolException(
            String.format(
                "Invalid pool state! Pool has negative count of invalidated connections!"
                    + " Unavailable: %s, reconnecting: %s, invalidating: %s",
                localUnavailable, localReconnecting, totalInvalidating)) {};
      }
      return totalInvalidating;
    }

    void incrementUnavailable() {
      long stamp = lock.writeLock();
      try {
        this.unavailable++;
      } finally {
        lock.unlockWrite(stamp);
      }
    }

    void decrementUnavailable() {
      long stamp = lock.writeLock();
      try {
        this.unavailable--;
      } finally {
        lock.unlockWrite(stamp);
      }
    }

    void incrementReconnecting() {
      long stamp = lock.writeLock();
      try {
        this.reconnecting++;
      } finally {
        lock.unlockWrite(stamp);
      }
    }

    void decrementReconnecting() {
      long stamp = lock.writeLock();
      try {
        this.reconnecting--;
      } finally {
        lock.unlockWrite(stamp);
      }
    }

    public void setTotal(int newTotal) {
      long stamp = lock.writeLock();
      try {
        this.total = newTotal;
      } finally {
        lock.unlockWrite(stamp);
      }
    }

    public int total() {
      return this.total;
    }
  }

  static final class PoolMetrics {

    static final String POOL_SIZE_METRIC = "pool.size";
    static final String POOL_SIZE_METRIC_DESC = "Total number of connections at the moment";
    private final Gauge poolSize;

    static final String POOL_AVAILABLE_METRIC = "pool.available";
    static final String POOL_AVAILABLE_METRIC_DESC =
        "Total number of connections available at the moment";
    private final Gauge poolAvailable;

    static final String POOL_UNAVAILABLE_METRIC = "pool.unavailable";
    static final String POOL_UNAVAILABLE_METRIC_DESC =
        "Total number of connection unavailable at the moment";
    private final Gauge poolUnavailable;

    static final String POOL_INVALIDATED_METRIC = "pool.invalidated";
    static final String POOL_INVALIDATED_METRIC_DESC =
        "Total number of invalidated connections at the moment";
    private final Gauge poolInvalidated;

    static final String POOL_RECONNECTING_METRIC = "pool.reconnecting";
    static final String POOL_RECONNECTING_METRIC_DESC =
        "Total number of reconnecting connections at the moment";
    private final Gauge poolReconnecting;

    static final String POOL_REQUEST_SUCCESS_METRIC = "pool.request.success";
    static final String POOL_REQUEST_SUCCESS_METRIC_DESC =
        "Total count of successful connections requests from pool";

    /** Count of successful connection requests. */
    private final Counter poolRequestSuccess;

    static final String POOL_REQUEST_ERRORS_METRIC = "pool.request.errors";
    static final String POOL_REQUEST_ERRORS_METRIC_DESC =
        "Total count of failed connection requests from pool";

    /** Count of failed connection requests. */
    private final Counter poolRequestErrors;

    static final String POOL_REQUEST_MISSES_METRIC = "pool.request.misses";
    static final String POOL_REQUEST_MISSES_METRIC_DESC =
        "Total count of connection requests with wrong tag or index";

    /** Count of connection requests for wrong tag or index. */
    private final Counter poolRequestMisses;

    static final String POOL_REQUEST_UNAVAILABLE_METRIC = "pool.request.unavail";
    static final String POOL_REQUEST_UNAVAILABLE_METRIC_DESC =
        "Total count of connection request finished with locked connect";

    /**
     * Count of connection requests for connections which were invalidated or in reconnecting state.
     */
    private final Counter poolRequestUnavailable;

    private final PoolEntryMetrics poolEntryMetrics;

    PoolMetrics(MeterRegistry registry, ConnectionStatistics statistics) {
      this.poolSize =
          Gauge.builder(POOL_SIZE_METRIC, statistics::total)
              .description(POOL_SIZE_METRIC_DESC)
              .register(registry);

      this.poolAvailable =
          Gauge.builder(POOL_AVAILABLE_METRIC, statistics::availableConnections)
              .description(POOL_AVAILABLE_METRIC_DESC)
              .register(registry);

      this.poolUnavailable =
          Gauge.builder(POOL_UNAVAILABLE_METRIC, statistics::unavailable)
              .description(POOL_UNAVAILABLE_METRIC_DESC)
              .register(registry);

      this.poolInvalidated =
          Gauge.builder(POOL_INVALIDATED_METRIC, statistics::invalidatedConnections)
              .description(POOL_INVALIDATED_METRIC_DESC)
              .register(registry);

      this.poolReconnecting =
          Gauge.builder(POOL_RECONNECTING_METRIC, statistics::reconnecting)
              .description(POOL_RECONNECTING_METRIC_DESC)
              .register(registry);

      this.poolRequestSuccess =
          Counter.builder(POOL_REQUEST_SUCCESS_METRIC)
              .description(POOL_REQUEST_SUCCESS_METRIC_DESC)
              .register(registry);

      this.poolRequestErrors =
          Counter.builder(POOL_REQUEST_ERRORS_METRIC)
              .description(POOL_REQUEST_ERRORS_METRIC_DESC)
              .register(registry);

      this.poolRequestMisses =
          Counter.builder(POOL_REQUEST_MISSES_METRIC)
              .description(POOL_REQUEST_MISSES_METRIC_DESC)
              .register(registry);

      this.poolRequestUnavailable =
          Counter.builder(POOL_REQUEST_UNAVAILABLE_METRIC)
              .description(POOL_REQUEST_UNAVAILABLE_METRIC_DESC)
              .register(registry);

      this.poolEntryMetrics = new PoolEntryMetrics(registry);
    }
  }
}

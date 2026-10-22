/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.msgpack.value.impl.ImmutableBooleanValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_DATA;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.IProtoClientImpl;
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.connection.ConnectionCloseEvent;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.core.connection.exceptions.ConnectionException;
import io.tarantool.core.protocol.Handlers;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.IProtoClientPoolImpl.ConnectionStatistics;

/**
 * Client/connection controller for pool.
 *
 * <p>This class manages its own instance of {@link io.tarantool.core.IProtoClient}, starts
 * heartbeats if need, controls invalidation and reconnection process.
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
final class PoolEntry {

  /**
   * IProto options for first request for checking connection liveness.
   *
   * @see io.tarantool.core.protocol.IProtoRequestOpts
   */
  private static final IProtoRequestOpts firstPingOpts =
      IProtoRequestOpts.empty().withRequestTimeout(1000);

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(PoolEntry.class);

  /** Deque for sliding window. */
  private final ArrayDeque<Integer> window;

  private final ConnectionStatistics connectionStatistics;

  /**
   * Options for heartbeats.
   *
   * @see io.tarantool.pool.HeartbeatOpts
   */
  private final HeartbeatOpts heartbeatOpts;

  /** Instance of {@link io.tarantool.core.IProtoClient}. */
  private final IProtoClient client;

  /**
   * IProto options for ping requests,
   *
   * @see io.tarantool.core.protocol.IProtoRequestOpts
   */
  private final IProtoRequestOpts heartbeatPingOpts;

  /** Handlers for request/response lifecycle events. */
  private final Handlers handlers;

  /** Instance of {@link io.tarantool.pool.InstanceConnectionGroup}. */
  private final InstanceConnectionGroup group;

  /** Tag from group. */
  private final String tag;

  /**
   * Instance of netty HashedWheelTimer.
   *
   * <p>Required for heartbeats and reconnecting tasks.
   */
  private final Timer timerService;

  /** Flag for graceful shutdown handling. */
  private final boolean gracefulShutdown;

  /** Index of entry within group. */
  private final int index;

  /** Time in milliseconds after that reconnect task will be executed. */
  private volatile long reconnectAfter;

  /** Parameter that will say tarantool not to use Tuple Extension. */
  private final boolean useTupleExtension;

  /** Metrics registry. */
  private final MeterRegistry metricsRegistry;

  /** Optional listener for pool events. */
  private final PoolEventListener poolEventListener;

  /**
   * Connection future.
   *
   * <p>It will be returned to all out clients wanting to obtain this client. It is recreated only
   * if client is reconnected.
   */
  private CompletableFuture<IProtoClient> connectFuture;

  /** Last heartbeat state/event. */
  private HeartbeatEvent lastHeartbeatEvent;

  /** Heartbeat timer/task. */
  private Timeout heartbeatTask;

  /** Reconnection task. */
  private Timeout reconnectTask;

  /** Flag signaling if heartbeat started or not. */
  private boolean isHeartbeatStarted;

  /**
   * Flag signaling if connection is available or not.
   *
   * <p>When connection comes to invalidated state or killed, pool entry is locked and connection
   * will not be returned to outer client.
   */
  private volatile boolean isLocked;

  /** Count of failed pings occurred in invalidated state. */
  private int currentDeathPings;

  /** Count of failed pings within window. */
  private int currentFailedPings;

  /** Unix timestamp for correcting ping scheduling. */
  private long lastPingTs;

  /**
   * Connect timeout in milliseconds.
   *
   * <p>Sets by pool.
   */
  private long connectTimeout;

  /** Metrics of {@link PoolEntry}. */
  private final PoolEntryMetrics metrics;

  /**
   * Constructor for PoolEntry.
   *
   * @param factory instance of {@link io.tarantool.core.connection.ConnectionFactory}
   * @param timerService instance of HashedWheelTimer
   * @param group instance of {@link io.tarantool.pool.InstanceConnectionGroup}
   * @param index index of pool entry within group
   * @param gracefulShutdown a boolean flag for graceful shutdown
   * @param connectTimeout connection timeout in milliseconds
   * @param reconnectAfter reconnect time in milliseconds
   * @param heartbeatOpts options for heartbeats, instance of {@link
   *     io.tarantool.pool.HeartbeatOpts}
   * @param watcherOpts options for connection watcher, instance of {@link
   *     io.tarantool.core.WatcherOptions}
   * @param connectionStatistics statistics on unavailable and reconnecting connections. Class is
   *     thread-safe.
   * @param registry instance of {@code io.micrometer.core.instrument.MeterRegistry}
   * @param ignoredPacketsHandler callback for accepting packets which were ignored by IProtoClient
   *     connections.
   * @param useTupleExtension use TUPLE_EXT feature if true.
   */
  public PoolEntry(
      ConnectionFactory factory,
      Timer timerService,
      InstanceConnectionGroup group,
      int index,
      boolean gracefulShutdown,
      long connectTimeout,
      long reconnectAfter,
      HeartbeatOpts heartbeatOpts,
      WatcherOptions watcherOpts,
      ConnectionStatistics connectionStatistics,
      MeterRegistry registry,
      TripleConsumer<String, Integer, IProtoResponse> ignoredPacketsHandler,
      Handlers handlers,
      boolean useTupleExtension,
      PoolEventListener poolEventListener,
      PoolEntryMetrics metrics) {
    this.metrics = metrics;
    this.metricsRegistry = registry;
    this.handlers = handlers;
    this.client =
        new IProtoClientImpl(
            factory,
            timerService,
            watcherOpts,
            registry,
            group.getFlushConsolidationHandler(),
            useTupleExtension);
    this.client.withHandlers(handlers);
    // heartbeat related
    this.isHeartbeatStarted = false;
    this.lastHeartbeatEvent = HeartbeatEvent.KILL;
    this.heartbeatOpts = heartbeatOpts;
    if (heartbeatOpts != null) {
      this.window = new ArrayDeque<>(heartbeatOpts.getWindowSize());
      this.heartbeatPingOpts =
          IProtoRequestOpts.empty().withRequestTimeout(heartbeatOpts.getPingInterval());
    } else {
      this.window = null;
      this.heartbeatPingOpts = null;
    }
    this.connectTimeout = connectTimeout;
    this.gracefulShutdown = gracefulShutdown;
    this.group = group;
    this.index = index;
    this.isLocked = false;
    this.reconnectAfter = reconnectAfter;
    this.tag = group.getTag();
    this.timerService = timerService;
    this.connectionStatistics = connectionStatistics;
    this.client.onClose(ConnectionCloseEvent.CLOSE_BY_REMOTE, this::handleConnectError);
    this.client.onClose(ConnectionCloseEvent.CLOSE_BY_SHUTDOWN, this::handleConnectError);
    this.useTupleExtension = useTupleExtension;
    this.poolEventListener = poolEventListener;

    if (ignoredPacketsHandler != null) {
      client.onIgnoredPacket(packet -> ignoredPacketsHandler.accept(this.tag, this.index, packet));
    }
  }

  /**
   * Getter for {@link io.tarantool.core.IProtoClient} instance.
   *
   * @return instance of {@link io.tarantool.core.IProtoClient}
   */
  public IProtoClient getClient() {
    return client;
  }

  /**
   * Method for locking pool entry.
   *
   * <p>Also increments count of unavailable clients.
   */
  public void lock() {
    if (!isLocked) {
      this.connectionStatistics.incrementUnavailable();
      isLocked = true;
    }
  }

  /**
   * Method for unlocking pool entry.
   *
   * <p>Also decrements count of unavailable clients and cancels reconnect task.
   */
  public void unlock() {
    if (isLocked) {
      stopReconnectTask();
      // Flag first, counter second: a reader that sees the counter decremented must not
      // still see the entry as locked.
      isLocked = false;
      this.connectionStatistics.decrementUnavailable();
    }
  }

  /**
   * Getter for {@link #isLocked}.
   *
   * @return {@link #isLocked} value.
   */
  public boolean isLocked() {
    return isLocked;
  }

  /** Closes client and stops heartbeat and reconnect tasks if started. */
  public void close() {
    stopReconnectTask();
    shutdown();
  }

  /** Closes client and stops heartbeat task is started. */
  public void shutdown() {
    connectFuture = null;
    stopHeartbeat();
    try {
      client.close();
    } catch (Exception e) {
      log.warn("Cannot close client in pool", e);
    }
    emit(listener -> listener.onConnectionClosed(tag, index));
  }

  /**
   * Start client connection process and returns futures.
   *
   * @return {@link java.util.concurrent.CompletableFuture} with client
   */
  public synchronized CompletableFuture<IProtoClient> connect() {
    if (connectFuture != null) {
      return connectFuture;
    }
    return internalConnect();
  }

  /**
   * Setter for {@link #connectTimeout}.
   *
   * @param timeout connect timeout in milliseconds
   */
  public void setConnectTimeout(long timeout) {
    this.connectTimeout = timeout;
  }

  /**
   * Setter for {@link #reconnectAfter}.
   *
   * @param reconnectAfter reconnect time in milliseconds
   */
  public void setReconnectAfter(long reconnectAfter) {
    this.reconnectAfter = reconnectAfter;
  }

  /** Heartbeat starter. */
  public void startHeartbeat() {
    if (isHeartbeatStarted || heartbeatOpts == null) {
      return;
    }
    log.info(
        "heartbeat: start for {}/{}: failures = {}, interval = {}, death pings = {}",
        tag,
        index,
        heartbeatOpts.getInvalidationThreshold(),
        heartbeatOpts.getPingInterval(),
        heartbeatOpts.getDeathThreshold());
    isHeartbeatStarted = true;
    window.clear();
    // heartbeat is being started immediately after first check so it is
    // not neccessary to send yet another ping again but just put success
    // mark to window
    window.addFirst(0);
    currentDeathPings = 0;
    currentFailedPings = 0;
    fire(HeartbeatEvent.ACTIVATE);
    heartbeatTask =
        timerService.newTimeout(this::ping, heartbeatOpts.getPingInterval(), TimeUnit.MILLISECONDS);
  }

  /** Heartbeat stopper. */
  public void stopHeartbeat() {
    isHeartbeatStarted = false;
    if (heartbeatTask != null) {
      heartbeatTask.cancel();
      heartbeatTask = null;
    }
  }

  /**
   * Internal method used by reconnect task and public connect.
   *
   * @return {@link java.util.concurrent.CompletableFuture} with client
   */
  private CompletableFuture<IProtoClient> internalConnect() {
    log.info("connect {}/{}", tag, index);
    final LongTaskTimer.Sample timer =
        this.metrics != null ? this.metrics.poolConnectTime.start() : null;
    CompletableFuture<?> future =
        client.connect(group.getAddress(), connectTimeout, gracefulShutdown);
    // Stop the timer on failure too, otherwise every failed attempt leaves an active sample.
    future.whenComplete((r, exc) -> PoolEntryMetrics.stopTimer(timer));
    String user = group.getUser();
    connectFuture =
        future
            .thenCompose(
                greeting -> {
                  if (user != null) {
                    return client.authorize(user, group.getPassword(), group.getAuthType());
                  }
                  return client.ping(firstPingOpts);
                })
            .thenCompose(r -> client.awaitConnectionReady())
            .thenApply(r -> client)
            .whenComplete(this::onConnectComplete);
    return connectFuture;
  }

  /**
   * Callback for {@link java.util.concurrent.CompletableFuture#whenComplete}.
   *
   * <p>Handles connection error if occurred, starts heartbeat.
   *
   * @param r any result of connection operation
   * @param exc exception occurred during connection process
   */
  private void onConnectComplete(Object r, Throwable exc) {
    if (this.metrics != null) {
      this.metrics.incrementPoolConnectMetrics(exc);
    }

    if (exc != null) {
      handleConnectError(r, exc);
      return;
    }
    startHeartbeat();
    unlock();
    log.info("connected {}/{}", tag, index);
    emit(listener -> listener.onConnectionOpened(tag, index));
  }

  /**
   * Handler for connection close.
   *
   * @param r connection instance
   * @param exc exception which led to connection close
   */
  private void handleConnectError(Object r, Throwable exc) {
    if (exc == null) {
      return;
    }
    Throwable failure = exc.getCause() != null ? exc.getCause() : exc;
    connectFuture = null;
    log.error("connect error {}/{}: {}", tag, index, failure.toString());
    emit(listener -> listener.onConnectionFailed(tag, index, failure));
    lock();
    shutdown();
    connectAfter();
  }

  /**
   * Reconnect task scheduler.
   *
   * <p>One failed attempt can be handled more than once (for example, by the connect future and by
   * a close event). A new task replaces the previous one, so the entry never has more than one
   * pending reconnect.
   */
  private void connectAfter() {
    log.info("reconnect {}/{} after {} ms", tag, index, reconnectAfter);
    synchronized (this) {
      if (reconnectTask == null) {
        this.connectionStatistics.incrementReconnecting();
      } else {
        reconnectTask.cancel();
      }
      reconnectTask =
          timerService.newTimeout(
              timeout -> internalConnect(), reconnectAfter, TimeUnit.MILLISECONDS);
    }
    emit(listener -> listener.onReconnectScheduled(tag, index, reconnectAfter));
  }

  /**
   * Run ping request.
   *
   * @param handler timeout handler
   */
  private void ping(Timeout handler) {
    lastPingTs = System.currentTimeMillis();
    final LongTaskTimer.Sample timer =
        this.metrics != null ? this.metrics.heartbeatTime.start() : null;
    heartbeatOpts
        .getPingFunction()
        .apply(client, this.heartbeatPingOpts)
        .whenComplete(
            (r, exc) -> {
              PoolEntryMetrics.stopTimer(timer);
              pong(r, exc);
            });
  }

  /** Schedules next ping. */
  private void nextPing() {
    long delta = heartbeatOpts.getPingInterval() - (System.currentTimeMillis() - lastPingTs);
    if (delta <= 0) {
      ping(null);
    } else {
      heartbeatTask = timerService.newTimeout(this::ping, delta, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Handler for ping response.
   *
   * @param result result of ping operation
   * @param exc exception
   */
  private void pong(IProtoResponse result, Throwable exc) {
    nextPing();

    int failure = 0;
    if (exc != null) {
      failure = 1;
      Throwable error = exc.getCause();
      if (error instanceof ConnectionException) {
        String msg = error.getMessage();
        if (msg.startsWith("Connection")) {
          stopHeartbeat();
          return;
        }
      }
    } else if (!result.isBodyEmpty()
        && !result.getBodyArrayValue(IPROTO_DATA).get(0).equals(ImmutableBooleanValueImpl.TRUE)) {
      failure = 1;
    }
    PoolEntryMetrics.incrementHeartbeatMetrics(this.metrics, failure);

    window.addFirst(failure);
    currentFailedPings += failure;
    if (window.size() > heartbeatOpts.getWindowSize()) {
      currentFailedPings -= window.removeLast();
    } else if (window.size() < heartbeatOpts.getWindowSize()) {
      return;
    }

    if (currentFailedPings >= heartbeatOpts.getInvalidationThreshold()) {
      fire(HeartbeatEvent.INVALIDATE);
      currentDeathPings++;
      if (currentDeathPings > heartbeatOpts.getDeathThreshold()) {
        fire(HeartbeatEvent.KILL);
      }
      return;
    }

    fire(HeartbeatEvent.ACTIVATE);
    currentDeathPings = 0;
  }

  /**
   * Switcher for connection states.
   *
   * @param event value of {@link io.tarantool.pool.HeartbeatEvent} enum.
   */
  private void fire(HeartbeatEvent event) {
    if (lastHeartbeatEvent == event) {
      return;
    }

    lastHeartbeatEvent = event;
    emit(listener -> listener.onHeartbeatEvent(tag, index, event));
    switch (event) {
      case INVALIDATE:
        lock();
        log.info("heartbeat: connection {}/{} is invalid", tag, index);
        break;
      case ACTIVATE:
        unlock();
        log.info("heartbeat: connection {}/{} is ok", tag, index);
        break;
      case KILL:
        shutdown();
        log.warn("heartbeat: close connection {}/{}", tag, index);
        connectAfter();
        break;
    }
  }

  /** Stops reconnecting task if it is active. */
  private synchronized void stopReconnectTask() {
    if (reconnectTask != null) {
      this.connectionStatistics.decrementReconnecting();
      reconnectTask.cancel();
      reconnectTask = null;
    }
  }

  /**
   * Calls listener callback if listener present.
   *
   * @param action action to perform
   */
  private void emit(Consumer<PoolEventListener> action) {
    if (poolEventListener != null) {
      action.accept(poolEventListener);
    }
  }

  /** {@link PoolEntry} metrics. */
  static class PoolEntryMetrics {

    static final String POOL_HEARTBEAT_SUCCESS_METRIC = "pool.heartbeat.success";
    static final String POOL_HEARTBEAT_SUCCESS_METRIC_DESC =
        "Total count of successful heartbeat ping requests";

    /** Count of successful heartbeat ping requests. */
    private final Counter heartbeatSuccess;

    static final String POOL_HEARTBEAT_ERRORS_METRIC = "pool.heartbeat.errors";
    static final String POOL_HEARTBEAT_ERRORS_METRIC_DESC =
        "Total count of failed heartbeat ping requests";

    /** Count of failed heartbeat ping requests. */
    private final Counter heartbeatErrors;

    static final String POOL_HEARTBEAT_TIME_METRIC = "pool.heartbeat.time";
    static final String POOL_HEARTBEAT_TIME_METRIC_DESC = "Time of ping request";

    /** Time of ping request. */
    private final LongTaskTimer heartbeatTime;

    static final String POOL_CONNECT_SUCCESS_METRIC = "pool.connect.success";
    static final String POOL_CONNECT_SUCCESS_METRIC_DESC = "Total count of succeeded connects";

    /** Count of successful connect attempts. */
    private final Counter poolConnectSuccess;

    static final String POOL_CONNECT_ERRORS_METRIC = "pool.connect.errors";
    static final String POOL_CONNECT_ERRORS_METRIC_DESC = "Total count of connect failures";

    /** Count of failed connect attempts. */
    private final Counter poolConnectErrors;

    static final String POOL_CONNECT_TIME_METRIC = "pool.connect.time";
    static final String POOL_CONNECT_TIME_METRIC_DESC = "Total time of connects";

    /** Time of connect. */
    private final LongTaskTimer poolConnectTime;

    public PoolEntryMetrics(MeterRegistry registry) {

      this.heartbeatSuccess =
          Counter.builder(POOL_HEARTBEAT_SUCCESS_METRIC)
              .description(POOL_HEARTBEAT_SUCCESS_METRIC_DESC)
              .register(registry);

      this.heartbeatErrors =
          Counter.builder(POOL_HEARTBEAT_ERRORS_METRIC)
              .description(POOL_HEARTBEAT_ERRORS_METRIC_DESC)
              .register(registry);

      this.heartbeatTime =
          LongTaskTimer.builder(POOL_HEARTBEAT_TIME_METRIC)
              .description(POOL_HEARTBEAT_TIME_METRIC_DESC)
              .register(registry);

      this.poolConnectSuccess =
          Counter.builder(POOL_CONNECT_SUCCESS_METRIC)
              .description(POOL_CONNECT_SUCCESS_METRIC_DESC)
              .register(registry);

      this.poolConnectErrors =
          Counter.builder(POOL_CONNECT_ERRORS_METRIC)
              .description(POOL_CONNECT_ERRORS_METRIC_DESC)
              .register(registry);

      this.poolConnectTime =
          LongTaskTimer.builder(POOL_CONNECT_TIME_METRIC)
              .description(POOL_CONNECT_TIME_METRIC_DESC)
              .register(registry);
    }

    /**
     * Increment {@link #poolConnectSuccess} if exception is null and increment {@link
     * #poolConnectErrors} if exception is not null.
     */
    private void incrementPoolConnectMetrics(Throwable exception) {
      if (exception != null) {
        this.poolConnectErrors.increment();
      } else {
        this.poolConnectSuccess.increment();
      }
    }

    /**
     * Increments heartbeat related metrics.
     *
     * @param fail if ping is failed then this value is 1, otherwise this value is 0.
     */
    private static void incrementHeartbeatMetrics(PoolEntryMetrics metrics, int fail) {
      if (metrics == null) {
        return;
      }

      if (fail != 0) {
        metrics.heartbeatErrors.increment();
      } else {
        metrics.heartbeatSuccess.increment();
      }
    }

    /**
     * If timer span is not null, stops time measurement.
     *
     * @param timer an instance of {@code LongTaskTimer.Sample}
     */
    private static void stopTimer(LongTaskTimer.Sample timer) {
      if (timer != null) {
        timer.stop();
      }
    }
  }
}

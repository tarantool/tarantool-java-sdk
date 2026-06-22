/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
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

  /**
   * Atomic variable passed from pool for counting unavailable connections.
   *
   * <p>When connection becomes unavailable, PoolEntry increments it.
   */
  private final AtomicInteger unavailable;

  /**
   * Atomic variable passed from pool for counting connections in reconnecting state.
   *
   * <p>When connection is being closed and reconnect started, PoolEntry increments it.
   */
  private final AtomicInteger reconnecting;

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

  /** Death threshold from {@link io.tarantool.pool.HeartbeatOpts}. */
  private final int deathThreshold;

  /** Count of ping failed, computed from window size and percent of failures. */
  private final int failedPingsThreshold;

  /** Index of entry within group. */
  private final int index;

  /** Window size from {@link io.tarantool.pool.HeartbeatOpts}. */
  private final int windowSize;

  /** Time in milliseconds after that reconnect task will be executed. */
  private final long reconnectAfter;

  /**
   * Function that send IProto packet that will be sent to tarantool. The response of this packet
   * will be considered as pong result.
   */
  private final BiFunction<IProtoClient, IProtoRequestOpts, CompletableFuture<IProtoResponse>>
      pingFunction;

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
  private volatile HeartbeatEvent lastHeartbeatEvent;

  /** Heartbeat timer/task. */
  private volatile Timeout heartbeatTask;

  /** Reconnection task. */
  private Timeout reconnectTask;

  /** Flag signaling if heartbeat started or not. */
  private volatile boolean isHeartbeatStarted;

  /**
   * Flag signaling if connection is available or not.
   *
   * <p>When connection comes to invalidated state or killed, pool entry is locked and connection
   * will not be returned to outer client.
   */
  private final AtomicBoolean isLocked = new AtomicBoolean(false);

  /**
   * Per-generation idempotency flag for {@link #shutdown()} close-event emit; reset in {@link
   * #internalConnect()} when a new connection generation begins.
   */
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);

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

  /** Count of successul connect attempts. */
  private Counter connectSuccess;

  /** Count of failed connect attempts. */
  private Counter connectErrors;

  /** Time of connect. */
  private LongTaskTimer connectTime;

  /** Count of successful heartbeat ping requests. */
  private Counter heartbeatSuccess;

  /** Count of failed heartbeat ping requests. */
  private Counter heartbeatErrors;

  /** Time of ping request. */
  private LongTaskTimer heartbeatTime;

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
   * @param unavailable atomic variable to count unavailable clients
   * @param reconnecting atomic variable to count client in reconnecting state
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
      AtomicInteger unavailable,
      AtomicInteger reconnecting,
      MeterRegistry registry,
      TripleConsumer<String, Integer, IProtoResponse> ignoredPacketsHandler,
      Handlers handlers,
      boolean useTupleExtension,
      PoolEventListener poolEventListener) {
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
    if (heartbeatOpts == null) {
      this.heartbeatOpts = null;
      this.heartbeatPingOpts = null;
      this.deathThreshold = 0;
      this.failedPingsThreshold = 0;
      this.windowSize = 0;
      this.window = null;
      this.pingFunction = null;
    } else {
      this.heartbeatOpts = heartbeatOpts;
      this.heartbeatPingOpts =
          IProtoRequestOpts.empty().withRequestTimeout(heartbeatOpts.getPingInterval());
      this.deathThreshold = heartbeatOpts.getDeathThreshold();
      this.failedPingsThreshold = heartbeatOpts.getInvalidationThreshold();
      this.windowSize = heartbeatOpts.getWindowSize();
      this.window = new ArrayDeque<>(this.windowSize);
      this.pingFunction = heartbeatOpts.getPingFunction();
    }
    this.connectTimeout = connectTimeout;
    this.gracefulShutdown = gracefulShutdown;
    this.group = group;
    this.index = index;
    this.reconnectAfter = reconnectAfter;
    this.tag = group.getTag();
    this.timerService = timerService;
    this.unavailable = unavailable;
    this.reconnecting = reconnecting;
    this.client.onClose(ConnectionCloseEvent.CLOSE_BY_REMOTE, this::handleConnectError);
    this.client.onClose(ConnectionCloseEvent.CLOSE_BY_SHUTDOWN, this::handleConnectError);
    this.useTupleExtension = useTupleExtension;
    this.poolEventListener = poolEventListener;

    if (ignoredPacketsHandler != null) {
      client.onIgnoredPacket(packet -> ignoredPacketsHandler.accept(this.tag, this.index, packet));
    }

    initMetrics();
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
   * Locks the pool entry and increments the pool-wide {@code unavailable} counter on the {@code
   * false → true} transition of {@link #isLocked}.
   *
   * <p>All callers run on Netty IO threads:
   *
   * <ul>
   *   <li>{@code ConnectFailure} — initial connect attempt fails; reaches this method via {@link
   *       #handleConnectError(Object, Throwable)} from the {@code CLOSE_BY_REMOTE} or {@code
   *       CLOSE_BY_SHUTDOWN} listener registered on {@link #client}.
   *   <li>{@code ConnectionBreak} — an established connection is closed by the remote side or shut
   *       down locally; same path as {@code ConnectFailure}.
   *   <li>{@code HeartbeatInvalidate} — the sliding-window failure rate in {@link #pong} crosses
   *       the invalidation threshold; reaches this method via {@link #fire(HeartbeatEvent)} for
   *       {@code INVALIDATE}.
   * </ul>
   *
   * <p>{@code ConnectionBreak} and {@code HeartbeatInvalidate} can fire concurrently (the heartbeat
   * decides to invalidate the connection at the same instant the close callback runs). The {@link
   * AtomicBoolean#compareAndSet} on {@link #isLocked} ensures only one of the two callers wins the
   * {@code false → true} transition, so {@code unavailable} is incremented at most once per lock
   * acquisition.
   */
  public void lock() {
    if (isLocked.compareAndSet(false, true)) {
      unavailable.incrementAndGet();
    }
  }

  /**
   * Unlocks the pool entry, cancels any pending reconnect task, and decrements the pool-wide {@code
   * unavailable} counter on the {@code true → false} transition of {@link #isLocked}.
   *
   * <p>Callers:
   *
   * <ul>
   *   <li>{@code HeartbeatResponse} — Netty IO thread, {@link #pong} reports a healthy response and
   *       {@link #fire(HeartbeatEvent)} for {@code ACTIVATE} reaches this method.
   *   <li>{@code ConnectSuccess} — Netty IO thread, {@link #onConnectComplete} reaches this method
   *       after a successful connect (and {@link #startHeartbeat}).
   *   <li>{@code UserConfigChange} — user code, when {@code pool.setGroups(...)} shrinks a group.
   *       Runs on the user thread while it holds {@code connectionPoolLock} (see {@code
   *       IProtoClientPoolImpl#shrinkGroup}); that lock serialises this path against other pool
   *       mutators, which is the only reason it is safe for user code to call {@code unlock()}
   *       directly — no other user-code path reaches this method. Rare in practice; not driven by
   *       Netty.
   * </ul>
   *
   * <p>{@code HeartbeatResponse} and {@code ConnectSuccess} cannot run at the same time for the
   * same entry: the heartbeat is started only after a successful connect, and {@code
   * HeartbeatResponse} only follows a prior {@code HeartbeatInvalidate} that already locked the
   * entry. The {@link AtomicBoolean#compareAndSet} ensures only one of possibly several concurrent
   * unlockers runs {@link #stopReconnectTask} and decrements {@code unavailable}.
   */
  public void unlock() {
    if (isLocked.compareAndSet(true, false)) {
      stopReconnectTask();
      unavailable.decrementAndGet();
    }
  }

  /**
   * Getter for {@link #isLocked}.
   *
   * @return {@link #isLocked} value.
   */
  public boolean isLocked() {
    return isLocked.get();
  }

  /** Closes client and stops heartbeat and reconnect tasks if started. */
  public void close() {
    stopReconnectTask();
    shutdown();
  }

  /**
   * Closes the underlying client and stops the heartbeat task.
   *
   * <p>Performs field mutations under the entry monitor, then releases it before calling {@code
   * client.close()} (which acquires the {@code ConnectionImpl} monitor) and emitting the close
   * event. Holding the entry monitor across either of those calls would create an ABBA deadlock
   * with the Netty close-callback path, which takes the {@code ConnectionImpl} monitor first and
   * then re-enters {@link #handleConnectError(Object, Throwable)} on the entry.
   *
   * <p>Callers:
   *
   * <ul>
   *   <li>{@code PoolClose} — user code, via {@code pool.close()} → {@link #close()} → this method.
   *       Runs on the user thread that closes the pool.
   *   <li>{@code ConnectError} — Netty IO thread, via {@link #handleConnectError(Object,
   *       Throwable)} which fires for both {@code ConnectFailure} (initial connect fails) and
   *       {@code ConnectionBreak} (established connection drops). Runs on the Netty IO thread
   *       delivering the close event.
   *   <li>{@code HeartbeatKill} — Netty IO thread, via {@link #fire(HeartbeatEvent)} for {@code
   *       KILL} when the death-ping counter crosses the death threshold. Runs on the Netty IO
   *       thread processing the heartbeat pong.
   * </ul>
   *
   * <p>The {@code isShutdown} {@link AtomicBoolean} guard on the {@code onConnectionClosed} emit
   * makes the listener invocation one-shot regardless of how many of the above paths reach this
   * method for the same entry.
   */
  public void shutdown() {
    synchronized (this) {
      connectFuture = null;
      stopHeartbeat();
    }
    try {
      client.close();
    } catch (Exception e) {
      log.warn("Cannot close client in pool", e);
    }
    if (isShutdown.compareAndSet(false, true)) {
      emit(listener -> listener.onConnectionClosed(tag, index));
    }
  }

  /**
   * Start client connection process and returns futures.
   *
   * @return {@link java.util.concurrent.CompletableFuture} with client
   */
  public CompletableFuture<IProtoClient> connect() {
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

  /** Heartbeat starter. */
  public void startHeartbeat() {
    if (isHeartbeatStarted || heartbeatOpts == null) {
      return;
    }
    log.info(
        "heartbeat: start for {}/{}: failures = {}, interval = {}, death pings = {}",
        tag,
        index,
        failedPingsThreshold,
        heartbeatOpts.getPingInterval(),
        deathThreshold);
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
   * <p>See {@link #shutdown()} for the monitor-ordering reasoning; {@code client.connect()} runs
   * outside the entry monitor for the same reason.
   *
   * @return {@link java.util.concurrent.CompletableFuture} with client
   */
  private CompletableFuture<IProtoClient> internalConnect() {
    synchronized (this) {
      if (connectFuture != null) {
        return connectFuture;
      }
    }
    log.info("connect {}/{}", tag, index);
    LongTaskTimer.Sample timer = startTimer(connectTime);
    CompletableFuture<?> future =
        client.connect(group.getAddress(), connectTimeout, gracefulShutdown);
    String user = group.getUser();
    CompletableFuture<IProtoClient> cf =
        future
            .thenCompose(
                greeting -> {
                  stopTimer(timer);
                  if (user != null) {
                    return client.authorize(user, group.getPassword(), group.getAuthType());
                  }
                  return client.ping(firstPingOpts);
                })
            .thenApply(r -> client);
    synchronized (this) {
      if (connectFuture != null) {
        return connectFuture;
      }
      connectFuture = cf;
      isShutdown.set(false);
    }
    cf.whenComplete(this::onConnectComplete);
    return cf;
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
    if (metricsRegistry != null) {
      if (exc != null) {
        connectErrors.increment();
      } else {
        connectSuccess.increment();
      }
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
    synchronized (this) {
      connectFuture = null;
    }
    log.error("connect error {}/{}: {}", tag, index, failure.toString());
    emit(listener -> listener.onConnectionFailed(tag, index, failure));
    lock();
    shutdown();
    connectAfter();
  }

  /** Reconnect task scheduler. */
  private void connectAfter() {
    synchronized (this) {
      log.info("reconnect {}/{} after {} ms", tag, index, reconnectAfter);
      if (reconnectTask != null) {
        // existing task is being replaced; the existing increment in `reconnecting` carries over
        // to the new task, so no counter change is needed here.
        reconnectTask.cancel();
      } else {
        reconnecting.incrementAndGet();
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
    LongTaskTimer.Sample timer = startTimer(heartbeatTime);
    pingFunction
        .apply(client, heartbeatPingOpts)
        .whenComplete(
            (r, exc) -> {
              stopTimer(timer);
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
    incHeartbeatCounters(failure);

    window.addFirst(failure);
    currentFailedPings += failure;
    if (window.size() > windowSize) {
      currentFailedPings -= window.removeLast();
    } else if (window.size() < windowSize) {
      return;
    }

    if (currentFailedPings >= failedPingsThreshold) {
      fire(HeartbeatEvent.INVALIDATE);
      currentDeathPings++;
      if (currentDeathPings > deathThreshold) {
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

  /**
   * Metrics initializations.
   *
   * <p>When registry is passed to {@link PoolEntry} constructor, metrics for heartbeats and connect
   * are created and registered in this registry.
   */
  private void initMetrics() {
    if (metricsRegistry == null) {
      return;
    }

    heartbeatSuccess = metricsRegistry.get("pool.heartbeat.success").counter();
    heartbeatErrors = metricsRegistry.get("pool.heartbeat.errors").counter();
    heartbeatTime = metricsRegistry.get("pool.heartbeat.time").longTaskTimer();

    connectSuccess = metricsRegistry.get("pool.connect.success").counter();
    connectErrors = metricsRegistry.get("pool.connect.errors").counter();
    connectTime = metricsRegistry.get("pool.connect.time").longTaskTimer();
  }

  /**
   * Creates timer span for time measurement.
   *
   * @param timer an instance of {@code LongTaskTimer}
   * @return instance of {@code LongTaskTimer.Sample}
   */
  private LongTaskTimer.Sample startTimer(LongTaskTimer timer) {
    if (metricsRegistry == null) {
      return null;
    }

    return timer.start();
  }

  /**
   * If timer span is not null, stops time measurement.
   *
   * @param timer an instance of {@code LongTaskTimer.Sample}
   */
  private void stopTimer(LongTaskTimer.Sample timer) {
    if (timer == null) {
      return;
    }

    timer.stop();
  }

  /**
   * Increments heartbeat related metrics.
   *
   * @param fail if ping is failed then this value is 1, otherwise this value is 0.
   */
  private void incHeartbeatCounters(int fail) {
    if (metricsRegistry == null || heartbeatSuccess == null || heartbeatErrors == null) {
      return;
    }

    if (fail != 0) {
      heartbeatErrors.increment();
    } else {
      heartbeatSuccess.increment();
    }
  }

  /** Stops reconnecting task if it is active. */
  private synchronized void stopReconnectTask() {
    if (reconnectTask != null) {
      reconnecting.decrementAndGet();
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
}

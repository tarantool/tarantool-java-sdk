/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.PoolEntryState;
import io.tarantool.pool.PoolEntryView;
import io.tarantool.pool.availability.AvailabilityPolicy;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StartEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StopEvent;

/**
 * The default availability policy: it sends pings to the connection and switches the connection
 * state between the active, invalidated and killed ones depending on the ping results.
 */
public class DefaultAvailabilityPolicy implements AvailabilityPolicy {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAvailabilityPolicy.class);

  private final AtomicReference<Utils> utils = new AtomicReference<>(null);

  private final Map<PoolEntryView, HeartbeatStateMachine> entries = new ConcurrentHashMap<>();

  private final HeartbeatOpts opts;

  public DefaultAvailabilityPolicy(HeartbeatOpts opts) {
    if (opts == null) {
      throw new IllegalArgumentException("Heartbeat options must not be null!");
    }
    this.opts = opts;
  }

  @Override
  public PoolEntryState initState(PoolEntryView view) {
    return HeartbeatConstants.ACTIVE_STATE;
  }

  @Override
  public Duration restartBackoff(PoolEntryView view) {
    return Duration.ZERO;
  }

  @Override
  public CompletionStage<Void> start(PoolEntryView view) {
    final CompletableFuture<Void> cf = new CompletableFuture<>();

    try {
      final HeartbeatStateMachine m =
          this.entries.computeIfAbsent(
              view, old -> new HeartbeatStateMachine(view, this.opts, this.utils.get()));
      m.handle(new StartEvent(cf));
    } catch (Throwable ex) {
      cf.completeExceptionally(ex);
    }
    return cf;
  }

  @Override
  public CompletionStage<Void> stop(PoolEntryView view, Throwable cause) {
    final CompletableFuture<Void> cf = new CompletableFuture<>();

    final HeartbeatStateMachine machine = this.entries.remove(view);
    if (machine == null) {
      cf.complete(null);
      return cf;
    }

    machine.handle(new StopEvent(cf, cause));
    return cf;
  }

  @Override
  public void configure(Timer timer, MeterRegistry metrics) {
    Objects.requireNonNull(timer, "timer must not be null");
    this.utils.compareAndSet(null, Utils.configure(timer, metrics));
  }

  boolean hasView(PoolEntryView view) {
    return this.entries.containsKey(view);
  }

  /**
   * The internal context of the policy: the timer and the metrics shared between all the
   * connections.
   */
  static final class Utils {

    private final Timer timer;
    private final MeterRegistry metrics;

    /** Count of successful heartbeat ping requests. */
    private final Counter heartbeatSuccess;

    /** Count of failed heartbeat ping requests. */
    private final Counter heartbeatErrors;

    /** Time of ping request. */
    private final LongTaskTimer heartbeatTime;

    private Utils(
        Timer timer,
        MeterRegistry metrics,
        Counter heartbeatSuccess,
        Counter heartbeatErrors,
        LongTaskTimer heartbeatTime) {
      this.timer = timer;
      this.metrics = metrics;
      this.heartbeatSuccess = heartbeatSuccess;
      this.heartbeatErrors = heartbeatErrors;
      this.heartbeatTime = heartbeatTime;
    }

    static Utils configure(Timer timer, MeterRegistry metrics) {
      Counter heartbeatSuccess = null;
      Counter heartbeatErrors = null;
      LongTaskTimer heartbeatTime = null;
      if (metrics != null) {
        heartbeatSuccess =
            Counter.builder(HeartbeatConstants.POOL_HEARTBEAT_SUCCESS)
                .description("Count of successful heartbeat ping requests")
                .register(metrics);

        heartbeatErrors =
            Counter.builder(HeartbeatConstants.POOL_HEARTBEAT_ERRORS)
                .description("Count of failed heartbeat ping requests")
                .register(metrics);

        heartbeatTime =
            LongTaskTimer.builder(HeartbeatConstants.POOL_HEARTBEAT_TIME)
                .description("Time of ping request")
                .register(metrics);
      }

      return new Utils(timer, metrics, heartbeatSuccess, heartbeatErrors, heartbeatTime);
    }

    /**
     * Creates timer span for time measurement.
     *
     * @return instance of {@code LongTaskTimer.Sample}
     */
    LongTaskTimer.Sample startHeartbeatTimer() {
      if (this.metrics == null) {
        return null;
      }

      return this.heartbeatTime.start();
    }

    /**
     * If timer span is not null, stops time measurement.
     *
     * @param timer an instance of {@code LongTaskTimer.Sample}
     */
    void stopHeartbeatTimer(LongTaskTimer.Sample timer) {
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
    void incHeartbeatCounters(int fail) {
      if (this.metrics == null) {
        return;
      }

      if (fail != 0) {
        heartbeatErrors.increment();
      } else {
        heartbeatSuccess.increment();
      }
    }

    Timer timer() {
      return this.timer;
    }
  }
}

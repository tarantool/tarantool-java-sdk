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

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.Timer;

import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.PoolEntryState;
import io.tarantool.pool.PoolEntryView;
import io.tarantool.pool.availability.AvailabilityPolicy;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.StartEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.StopEvent;

/**
 * The default availability policy: it sends pings to the connection and switches the connection
 * state between the active, invalidated and killed ones depending on the ping results.
 */
public class DefaultAvailabilityPolicy implements AvailabilityPolicy {

  private final AtomicReference<Utils> utils;

  private final HeartbeatOpts opts;

  private final Map<PoolEntryView, HeartbeatStateMachine> entries;

  public DefaultAvailabilityPolicy(HeartbeatOpts opts) {
    this.opts = Objects.requireNonNull(opts, "Heartbeat options must not be null!");
    this.utils = new AtomicReference<>(null);
    this.entries = new ConcurrentHashMap<>();
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
    Objects.requireNonNull(view, "view must not be null!");
    final CompletableFuture<Void> cf = new CompletableFuture<>();

    try {
      final HeartbeatStateMachine m =
          this.entries.computeIfAbsent(
              view, old -> new HeartbeatStateMachine(view, this.opts, this.utils.get()));
      m.handle(new StartEvent(cf));
    } catch (Exception ex) {
      cf.completeExceptionally(ex);
    }
    return cf;
  }

  @Override
  public CompletionStage<Void> stop(PoolEntryView view, Throwable cause) {
    Objects.requireNonNull(view, "view must not be null!");
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
}

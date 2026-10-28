/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.pool.PoolEntryState;
import io.tarantool.pool.PoolEntryView;
import io.tarantool.pool.availability.AvailabilityPolicy;

/**
 * A policy that neither checks nor modifies the connection availability state for the load
 * balancer. The initial state is always {@link #DEFAULT_ACTIVE}.
 */
public class DefaultNoopAvailabilityPolicy implements AvailabilityPolicy {

  private static final PoolEntryState DEFAULT_ACTIVE =
      new PoolEntryState("DEFAULT_ACTIVE", false) {};

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNoopAvailabilityPolicy.class);

  @Override
  public void configure(Timer timer, MeterRegistry metrics) {
    LOGGER.debug("[Noop policy]: configured with timer: {} and metrics: {}.", timer, metrics);
  }

  @Override
  public void initStateBeforeAll(PoolEntryView view) {
    view.changeState(DEFAULT_ACTIVE);
  }

  @Override
  public Duration restartBackoff(PoolEntryView view) {
    return Duration.ZERO;
  }

  @Override
  public CompletionStage<Void> start(PoolEntryView view) {
    LOGGER.debug("[Noop policy]: started.");
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletionStage<Void> stop(PoolEntryView view, Throwable cause) {
    LOGGER.debug("[Noop policy]: stopped.");
    return CompletableFuture.completedFuture(null);
  }
}

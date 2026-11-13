/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.state.AbstractDrainLoopMachine;
import io.tarantool.pool.HeartbeatEvent;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.PoolEntryView;
import io.tarantool.pool.availability.Metadata;
import io.tarantool.pool.availability.heartbeat.DefaultAvailabilityPolicy.Utils;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.KillEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.SendPingEvent;
import io.tarantool.pool.availability.heartbeat.exceptions.HeartbeatException;

/**
 * The internal state machine of the default availability policy. It is not a part of the public
 * API: the client must not have access to the machine, its states and its context.
 */
final class HeartbeatStateMachine extends AbstractDrainLoopMachine<PolicyEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatStateMachine.class);

  /*
   **********************************************************
   * Params
   **********************************************************
   */
  private final PoolEntryView view;

  private final Utils utils;

  private final HeartbeatOpts heartbeatOpts;

  private final IProtoRequestOpts heartbeatPingOpts;

  private final String identity;

  /*
   **********************************************************
   * State
   **********************************************************
   */
  private final CompletableFuture<Void> rootCloseFuture = new CompletableFuture<>();

  private final CompletableFuture<Void> rootStartFuture = new CompletableFuture<>();

  private final HeartbeatCtx ctx;

  private volatile PolicyState state = new NewState(this);

  HeartbeatStateMachine(PoolEntryView view, HeartbeatOpts opts, Utils utils) {
    Objects.requireNonNull(view, "view must not be null");
    Objects.requireNonNull(opts, "HeartbeatOpts must not be null!");
    Objects.requireNonNull(utils, "Policy is not configured!");
    Objects.requireNonNull(utils.timer(), "Timer must not be null!");

    this.view = view;
    this.utils = utils;
    final Metadata metadata = view.metadata();
    this.identity = metadata.settings().getTag() + '/' + metadata.index();

    this.heartbeatOpts = opts;
    final Deque<Integer> window = new ArrayDeque<>(opts.getWindowSize());
    this.heartbeatPingOpts = IProtoRequestOpts.empty().withRequestTimeout(opts.getPingInterval());
    this.ctx = new HeartbeatCtx(window, null, 0, 0, 0);
  }

  @Override
  protected void processEvent(PolicyEvent event) {
    this.state.handle(event);
  }

  @Override
  protected void onHandleError(Throwable error) {
    LOGGER.warn(
        "[{}]: caught an unexpected error while handling an event. Stopping the machine and"
            + " reconnecting...",
        this,
        error);
    final boolean isStarted =
        this.rootStartFuture.isDone() && !this.rootStartFuture.isCompletedExceptionally();
    if (isStarted) {
      this.view.changeState(HeartbeatConstants.KILL_STATE);
      handle(KillEvent.INSTANCE);
    } else {
      // The start has failed: do not leave a half-started machine (with a running ping timer)
      // behind. Move to the closed state and release the context.
      this.state = new StoppedState(this);
      this.view.changeState(HeartbeatConstants.INVALIDATE_STATE);
      this.ctx.clear();
      this.rootStartFuture.completeExceptionally(
          new HeartbeatException("Got unexpected errors", error));
    }
  }

  void state(PolicyState newState) {
    this.state = newState;
  }

  PolicyState state() {
    return this.state;
  }

  HeartbeatCtx ctx() {
    return this.ctx;
  }

  PoolEntryView view() {
    return this.view;
  }

  HeartbeatOpts opts() {
    return this.heartbeatOpts;
  }

  Utils utils() {
    return this.utils;
  }

  CompletableFuture<Void> rootCloseFuture() {
    return this.rootCloseFuture;
  }

  CompletableFuture<Void> rootStartFuture() {
    return this.rootStartFuture;
  }

  IProtoRequestOpts heartbeatPingRequestOpts() {
    return this.heartbeatPingOpts;
  }

  String identity() {
    return this.identity;
  }

  void linkFuture(CompletionStage<Void> root, CompletionStage<Void> linkable) {
    if (linkable == null) {
      return;
    }

    root.whenComplete(
        (_unused, e) -> {
          if (e == null) {
            linkable.toCompletableFuture().complete(null);
          } else {
            linkable.toCompletableFuture().completeExceptionally(e);
          }
        });
  }

  /**
   * The mutable part of the machine state. It is shared between the states and must be accessible
   * for them without any restrictions.
   */
  final class HeartbeatCtx {

    final Deque<Integer> window;

    Timeout heartbeatTask;

    int currentDeathPings;

    int currentFailedPings;

    /** Monotonic ({@link System#nanoTime()}) timestamp of the last sent ping. */
    long lastPingNanos;

    HeartbeatEvent lastEvent;

    HeartbeatCtx(
        Deque<Integer> window,
        Timeout heartbeatTask,
        int currentDeathPings,
        int currentFailedPings,
        long lastPingNanos) {
      this.window = window;
      this.heartbeatTask = heartbeatTask;
      this.currentDeathPings = currentDeathPings;
      this.currentFailedPings = currentFailedPings;
      this.lastPingNanos = lastPingNanos;
    }

    void clear() {
      cancelTimer();
      this.window.clear();
      this.heartbeatTask = null;
      this.currentDeathPings = 0;
      this.currentFailedPings = 0;
      this.lastPingNanos = 0;
      this.lastEvent = null;
    }

    void cancelTimer() {
      if (this.heartbeatTask != null) {
        this.heartbeatTask.cancel();
      }
    }

    void pingTimer(long delta) {
      cancelTimer();
      this.heartbeatTask =
          utils
              .timer()
              .newTimeout(_u -> handle(SendPingEvent.INSTANCE), delta, TimeUnit.MILLISECONDS);
    }
  }

  @Override
  public String toString() {
    return '[' + this.identity + "][HeartbeatStateMachine]";
  }
}

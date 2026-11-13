/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.LongTaskTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.connection.exceptions.ConnectionException;
import io.tarantool.core.exceptions.ShutdownException;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatEvent;
import io.tarantool.pool.PoolEntryView;
import io.tarantool.pool.availability.Metadata;
import io.tarantool.pool.availability.heartbeat.HeartbeatStateMachine.HeartbeatCtx;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.ActivateEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.InvalidateEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.KillEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.PongEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.SendPingEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StartEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StopEvent;

final class StartedState extends PolicyState {

  private static final Logger LOGGER = LoggerFactory.getLogger(StartedState.class);

  StartedState(HeartbeatStateMachine machine) {
    super(machine, "HEARTBEAT_STARTED");
  }

  @Override
  public void handle(PolicyEvent event) {
    super.handle(event);

    if (event instanceof StartEvent) {
      handleStart((StartEvent) event);
      return;
    }

    if (event instanceof StopEvent) {
      handleStop((StopEvent) event);
      return;
    }

    if (event instanceof SendPingEvent) {
      handleSendPing((SendPingEvent) event);
      return;
    }

    if (event instanceof PongEvent) {
      handlePong((PongEvent) event);
      return;
    }

    if (event instanceof ActivateEvent) {
      handleActivate(event);
      return;
    }

    if (event instanceof InvalidateEvent) {
      handleInvalidate(event);
      return;
    }

    if (event instanceof KillEvent) {
      handleKill(event);
      return;
    }

    logUnexpectedEvent(event);
  }

  private void handleStart(StartEvent event) {
    logStartHandleEvent(event);
    final CompletableFuture<Void> rootStartFuture = this.machine.rootStartFuture();
    this.machine.linkFuture(rootStartFuture, event.startFuture());
  }

  private void handleStop(StopEvent event) {
    logStartHandleEvent(event);

    final PoolEntryView view = this.machine.view();
    final HeartbeatCtx ctx = this.machine.ctx();

    final PolicyState newState = new StoppedState(this.machine);
    transitionTo(newState);
    view.changeState(HeartbeatConstants.INVALIDATE_STATE);
    ctx.clear();

    final CompletableFuture<Void> rootCloseFuture = this.machine.rootCloseFuture();
    this.machine.linkFuture(rootCloseFuture, event.closeFuture());

    if (event.cause() != null) {
      LOGGER.debug(
          "[{}][{}]: stop policy with cause", this.machine.identity(), this, event.cause());
    }
    if (!rootCloseFuture.isDone()) {
      rootCloseFuture.complete(null);
    }
    LOGGER.info("[{}][{}]: availability policy is stopped", this.machine.identity(), this);
  }

  private void handleActivate(PolicyEvent event) {
    logStartHandleEvent(event);

    final PoolEntryView view = this.machine.view();
    final Metadata metadata = view.metadata();
    final String tag = metadata.settings().getTag();
    final int idx = metadata.index();

    view.changeState(HeartbeatConstants.ACTIVE_STATE);

    this.machine.ctx().lastEvent = HeartbeatEvent.ACTIVATE;

    view.emit(l -> l.onHeartbeatEvent(tag, idx, HeartbeatEvent.ACTIVATE));
    LOGGER.info("[{}][{}]: connection is OK", this.machine.identity(), this);
  }

  private void handleInvalidate(PolicyEvent event) {
    logStartHandleEvent(event);

    final PoolEntryView view = this.machine.view();
    final String tag = view.metadata().settings().getTag();
    final int idx = view.metadata().index();

    view.changeState(HeartbeatConstants.INVALIDATE_STATE);
    this.machine.ctx().lastEvent = HeartbeatEvent.INVALIDATE;

    view.emit(l -> l.onHeartbeatEvent(tag, idx, HeartbeatEvent.INVALIDATE));
    LOGGER.info("[{}][{}]: connection is invalid", this.machine.identity(), this);
  }

  private void handleKill(PolicyEvent event) {
    logStartHandleEvent(event);

    final PoolEntryView view = this.machine.view();
    final String tag = view.metadata().settings().getTag();
    final int index = view.metadata().index();

    final HeartbeatCtx ctx = this.machine.ctx();
    final PolicyState newState = new StoppedState(this.machine);
    transitionTo(newState);
    ctx.clear();

    view.changeState(HeartbeatConstants.KILL_STATE);
    ctx.lastEvent = HeartbeatEvent.KILL;

    view.emit(l -> l.onHeartbeatEvent(tag, index, HeartbeatEvent.KILL));

    LOGGER.warn("[{}][{}]: close connection", this.machine.identity(), this);
    view.reconnect();
  }

  private void handleSendPing(SendPingEvent event) {
    logStartHandleEvent(event);

    final HeartbeatCtx heartbeatCtx = this.machine.ctx();
    final PoolEntryView view = this.machine.view();

    heartbeatCtx.lastPingNanos = System.nanoTime();

    final LongTaskTimer.Sample timer = this.machine.utils().startHeartbeatTimer();

    CompletableFuture<IProtoResponse> ping;
    try {
      ping =
          Objects.requireNonNull(
              this.machine
                  .opts()
                  .getPingFunction()
                  .apply(view.client(), this.machine.heartbeatPingRequestOpts()),
              "ping function returned null");
    } catch (Exception e) {
      LOGGER.debug(
          "[{}][{}]: caught an error while sending the ping...", this.machine.identity(), this, e);
      this.machine.utils().stopHeartbeatTimer(timer);
      this.machine.handle(new PongEvent(null, e));
      return;
    }

    ping.whenComplete(
        (result, ex) -> {
          this.machine.utils().stopHeartbeatTimer(timer);
          this.machine.handle(new PongEvent(result, ex));
        });
  }

  private void handlePong(PongEvent event) {
    logStartHandleEvent(event);
    final Throwable exc = event.exception();
    final IProtoResponse result = event.response();
    final HeartbeatCtx heartbeatCtx = this.machine.ctx();

    int failure = 0;
    Throwable error = null;

    if (exc != null) {
      failure = 1;
      error = unwrap(exc);
      LOGGER.debug("[{}][{}]: ping failed: {}", this.machine.identity(), this, exc.toString());
    } else if (!PingResult.hasSuccessPingResult(result, this.machine.identity())) {
      failure = 1;
    }

    this.machine.utils().incHeartbeatCounters(failure);

    if (isTerminalError(error)) {
      LOGGER.warn(
          "[{}][{}]: connection is dead: {}", this.machine.identity(), this, error.toString());
      this.machine.handle(KillEvent.INSTANCE);
      return;
    }

    nextPing();

    heartbeatCtx.window.addFirst(failure);
    heartbeatCtx.currentFailedPings += failure;
    if (heartbeatCtx.window.size() > this.machine.opts().getWindowSize()) {
      heartbeatCtx.currentFailedPings -= heartbeatCtx.window.removeLast();
    } else if (heartbeatCtx.window.size() < this.machine.opts().getWindowSize()) {
      return;
    }

    if (heartbeatCtx.currentFailedPings >= this.machine.opts().getInvalidationThreshold()) {
      if (heartbeatCtx.lastEvent != HeartbeatEvent.INVALIDATE) {
        this.machine.handle(InvalidateEvent.INSTANCE);
      }
      heartbeatCtx.currentDeathPings++;
      if (heartbeatCtx.currentDeathPings > this.machine.opts().getDeathThreshold()) {
        if (heartbeatCtx.lastEvent != HeartbeatEvent.KILL) {
          this.machine.handle(KillEvent.INSTANCE);
        }
      }
      return;
    }

    if (heartbeatCtx.lastEvent != HeartbeatEvent.ACTIVATE) {
      this.machine.handle(ActivateEvent.INSTANCE);
    }
    heartbeatCtx.currentDeathPings = 0;
  }

  private void nextPing() {
    final HeartbeatCtx ctx = this.machine.ctx();
    // The monotonic clock is used: the wall clock may jump (NTP, manual change).
    final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - ctx.lastPingNanos);
    final long delta = this.machine.opts().getPingInterval() - elapsedMillis;
    if (delta <= 0) {
      this.machine.handle(SendPingEvent.INSTANCE);
    } else {
      ctx.pingTimer(delta);
    }
  }

  private static Throwable unwrap(Throwable ex) {
    Throwable result = ex;
    while ((result instanceof CompletionException || result instanceof ExecutionException)
        && result.getCause() != null) {
      result = result.getCause();
    }
    return result;
  }

  // TODO decide which exceptions are terminal
  private static boolean isTerminalError(Throwable error) {
    return error instanceof ConnectionException
        || error instanceof ShutdownException
        || error instanceof IllegalStateException
        || error instanceof NullPointerException;
  }
}

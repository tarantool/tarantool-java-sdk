/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.LongTaskTimer;

import io.tarantool.core.connection.exceptions.ConnectionException;
import io.tarantool.core.exceptions.ShutdownException;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.utils.CFUtils;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.PoolEntryState;
import io.tarantool.pool.PoolEntryView;
import io.tarantool.pool.availability.Metadata;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.ActivateEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.InvalidateEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.KillEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.PongEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.SendPingEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.StartEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.StopEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatStateMachine.HeartbeatCtx;

final class RunningState extends HeartbeatState {

  RunningState(HeartbeatStateMachine machine) {
    super(machine, "HEARTBEAT_STARTED");
  }

  @Override
  protected void onStart(StartEvent event) {
    logStartHandleEvent(event);
    CFUtils.linkFuture(this.machine.rootStartFuture(), event.startFuture());
  }

  @Override
  protected void onStop(StopEvent event) {
    logStartHandleEvent(event);
    transitionTo(new StoppedState(this.machine));
    this.machine.view().changeState(HeartbeatConstants.INVALIDATE_STATE);
    this.machine.ctx().clear();
    completeStop(event);
    this.logger.info("[{}][{}]: availability policy is stopped", machine.identity(), this);
  }

  @Override
  protected void onActivate(ActivateEvent event) {
    emitStateChange(
        event,
        HeartbeatConstants.ACTIVE_STATE,
        io.tarantool.pool.HeartbeatEvent.ACTIVATE,
        "connection is OK");
  }

  @Override
  protected void onInvalidate(InvalidateEvent event) {
    emitStateChange(
        event,
        HeartbeatConstants.INVALIDATE_STATE,
        io.tarantool.pool.HeartbeatEvent.INVALIDATE,
        "connection is invalid");
  }

  @Override
  protected void onKill(KillEvent event) {
    transitionTo(new StoppedState(this.machine));
    this.machine.ctx().clear();
    emitStateChange(
        event, HeartbeatConstants.KILL_STATE, io.tarantool.pool.HeartbeatEvent.KILL, null);
    this.logger.warn("[{}][{}]: close connection", this.machine.identity(), this);
    this.machine.view().reconnect();
  }

  @Override
  protected void onSendPing(SendPingEvent event) {
    logStartHandleEvent(event);
    this.machine.ctx().lastPingNanos = System.nanoTime();
    final LongTaskTimer.Sample timer = this.machine.utils().beginHeartbeatTimeMetric();

    CompletableFuture<IProtoResponse> ping;
    try {
      ping =
          Objects.requireNonNull(
              this.machine
                  .opts()
                  .getPingFunction()
                  .apply(this.machine.view().client(), this.machine.heartbeatPingRequestOpts()),
              "ping function returned null");
    } catch (Exception e) {
      this.logger.debug(
          "[{}][{}]: caught an error while sending the ping...", this.machine.identity(), this, e);
      ping = new CompletableFuture<>();
      ping.completeExceptionally(e);
    }

    ping.whenComplete(
        (res, ex) -> {
          this.machine.utils().finishHeartbeatTimeMetric(timer);
          this.machine.handle(new PongEvent(res, ex));
        });
  }

  @Override
  protected void onPong(PongEvent event) {
    logStartHandleEvent(event);
    final Throwable error = CFUtils.unwrap(event.exception());
    final int failure =
        (error != null
                || !PingResult.hasSuccessPingResult(event.response(), this.machine.identity()))
            ? 1
            : 0;

    if (error != null) {
      this.logger.debug(
          "[{}][{}]: ping failed: {}", this.machine.identity(), this, event.exception().toString());
    }
    this.machine.utils().incHeartbeatCounters(failure);

    if (isTerminalError(error)) {
      this.logger.warn(
          "[{}][{}]: connection is dead: {}", this.machine.identity(), this, error.toString());
      this.machine.handle(KillEvent.INSTANCE);
      return;
    }

    nextPing();
    final HeartbeatCtx ctx = machine.ctx();
    final HeartbeatOpts opts = machine.opts();

    ctx.window.addFirst(failure);
    ctx.currentFailedPings += failure;
    if (ctx.window.size() > opts.getWindowSize()) {
      ctx.currentFailedPings -= ctx.window.removeLast();
    } else if (ctx.window.size() < opts.getWindowSize()) {
      return;
    }

    if (ctx.currentFailedPings >= opts.getInvalidationThreshold()) {
      compareWithLastAndSend(io.tarantool.pool.HeartbeatEvent.INVALIDATE, InvalidateEvent.INSTANCE);
      if (++ctx.currentDeathPings > opts.getDeathThreshold()) {
        compareWithLastAndSend(io.tarantool.pool.HeartbeatEvent.KILL, KillEvent.INSTANCE);
      }
    } else {
      compareWithLastAndSend(io.tarantool.pool.HeartbeatEvent.ACTIVATE, ActivateEvent.INSTANCE);
      ctx.currentDeathPings = 0;
    }
  }

  private void emitStateChange(
      HeartbeatEvent event,
      PoolEntryState viewState,
      io.tarantool.pool.HeartbeatEvent hbEvent,
      String infoMsg) {
    logStartHandleEvent(event);
    final PoolEntryView view = this.machine.view();
    final Metadata meta = view.metadata();
    view.changeState(viewState);
    this.machine.ctx().lastEvent = hbEvent;
    view.emit(l -> l.onHeartbeatEvent(meta.settings().getTag(), meta.index(), hbEvent));
    if (infoMsg != null) {
      this.logger.info("[{}][{}]: {}", this.machine.identity(), this, infoMsg);
    }
  }

  private void nextPing() {
    final long elapsed =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - machine.ctx().lastPingNanos);
    final long delta = machine.opts().getPingInterval() - elapsed;
    if (delta <= 0) {
      machine.handle(SendPingEvent.INSTANCE);
    } else {
      machine.ctx().scheduleNextPing(delta);
    }
  }

  private void compareWithLastAndSend(
      io.tarantool.pool.HeartbeatEvent expectedLast, HeartbeatEvent eventToSend) {
    if (machine.ctx().lastEvent != expectedLast) {
      machine.handle(eventToSend);
    }
  }

  // TODO decide which exceptions are terminal
  private static boolean isTerminalError(Throwable error) {
    return error instanceof ConnectionException
        || error instanceof ShutdownException
        || error instanceof IllegalStateException
        || error instanceof NullPointerException;
  }
}

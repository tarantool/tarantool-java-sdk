/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.availability.heartbeat.HeartbeatStateMachine.HeartbeatCtx;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.ActivateEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.InvalidateEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.KillEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.PongEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.SendPingEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StartEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StopEvent;

final class NewState extends PolicyState {

  private static final Logger LOGGER = LoggerFactory.getLogger(NewState.class);

  NewState(HeartbeatStateMachine machine) {
    super(machine, "HEARTBEAT_NEW");
  }

  @Override
  public void handle(PolicyEvent event) {
    super.handle(event);

    if (event instanceof PongEvent
        || event instanceof SendPingEvent
        || event instanceof ActivateEvent
        || event instanceof InvalidateEvent
        || event instanceof KillEvent) {
      return;
    }

    if (event instanceof StartEvent) {
      handleStart((StartEvent) event);
      return;
    }

    if (event instanceof StopEvent) {
      handleStop((StopEvent) event);
      return;
    }

    logUnexpectedEvent(event);
  }

  private void handleStart(StartEvent event) {
    logStartHandleEvent(event);

    final HeartbeatOpts opts = this.machine.opts();

    LOGGER.info(
        "[{}][{}]: start heartbeat with [failures = {}, interval = {}, death pings = {}]",
        this.machine.identity(),
        this,
        opts.getInvalidationThreshold(),
        opts.getPingInterval(),
        opts.getDeathThreshold());

    final HeartbeatCtx ctx = this.machine.ctx();
    ctx.window.clear();

    // The connection has just been established: the successful connection is the first ping, so
    // the window starts with a success mark and no extra ping is needed.
    ctx.window.addFirst(0);
    final PolicyState newState = new StartedState(this.machine);
    transitionTo(newState);

    final CompletableFuture<Void> rootStartFuture = this.machine.rootStartFuture();
    this.machine.linkFuture(rootStartFuture, event.startFuture());

    // TODO may throw
    ctx.pingTimer(opts.initialDelay().toMillis());

    // The library has just established the connection, so it is alive: the entry is activated
    // before the startup is reported to the library.
    newState.handle(ActivateEvent.INSTANCE);

    rootStartFuture.complete(null);
  }

  /**
   * This event can arrive in this state only if the stop method has been called between the
   * creation of the machine and the start call. In this case the context is not initialized yet and
   * there are no futures.
   */
  private void handleStop(StopEvent event) {
    logStartHandleEvent(event);

    final HeartbeatCtx ctx = this.machine.ctx();

    ctx.clear();
    final PolicyState newState = new StoppedState(this.machine);
    transitionTo(newState);
    this.machine.view().changeState(HeartbeatConstants.INVALIDATE_STATE);

    final CompletableFuture<Void> rootCloseFuture = this.machine.rootCloseFuture();
    this.machine.linkFuture(rootCloseFuture, event.closeFuture());

    if (event.cause() != null) {
      LOGGER.debug(
          "[{}][{}]: stop policy with cause", this.machine.identity(), this, event.cause());
    }

    if (!rootCloseFuture.isDone()) {
      rootCloseFuture.complete(null);
    }
  }
}

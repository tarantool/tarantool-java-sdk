/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.pool.availability.heartbeat.PolicyEvent.ActivateEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.InvalidateEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.KillEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.PongEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.SendPingEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StartEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StopEvent;
import io.tarantool.pool.availability.heartbeat.exceptions.HeartbeatClosedException;

final class StoppedState extends PolicyState {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoppedState.class);

  StoppedState(HeartbeatStateMachine machine) {
    super(machine, "HEARTBEAT_STOPPED");
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

    final CompletableFuture<Void> startFuture = event.startFuture();
    if (!startFuture.isDone()) {
      startFuture.completeExceptionally(new HeartbeatClosedException("Already closed!"));
    }
  }

  private void handleStop(StopEvent event) {
    logStartHandleEvent(event);

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

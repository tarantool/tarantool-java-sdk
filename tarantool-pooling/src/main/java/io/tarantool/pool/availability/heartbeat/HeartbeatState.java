/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.state.State;
import io.tarantool.core.utils.CFUtils;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.ActivateEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.InvalidateEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.KillEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.PongEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.SendPingEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.StartEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.StopEvent;

/**
 * The base class of the internal states of the default availability policy. A state works with the
 * machine and with its context without any restrictions.
 */
abstract class HeartbeatState implements State<HeartbeatEvent> {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final HeartbeatStateMachine machine;
  private final String name;

  HeartbeatState(HeartbeatStateMachine machine, String name) {
    this.machine = machine;
    this.name = name;
  }

  @Override
  public void handle(HeartbeatEvent event) {
    if (event == null) {
      this.logger.debug("[{}][{}]: got a null event. Skipping...", machine.identity(), this);
      return;
    }
    this.logger.debug("[{}][{}]: received '{}' event", machine.identity(), this, event);

    if (event instanceof StartEvent) onStart((StartEvent) event);
    else if (event instanceof StopEvent) onStop((StopEvent) event);
    else if (event instanceof SendPingEvent) onSendPing((SendPingEvent) event);
    else if (event instanceof PongEvent) onPong((PongEvent) event);
    else if (event instanceof ActivateEvent) onActivate((ActivateEvent) event);
    else if (event instanceof InvalidateEvent) onInvalidate((InvalidateEvent) event);
    else if (event instanceof KillEvent) onKill((KillEvent) event);
    else
      this.logger.debug(
          "[{}][{}]: received an unexpected '{}' event", machine.identity(), this, event);
  }

  protected void onStart(StartEvent e) {}

  protected void onStop(StopEvent e) {}

  protected void onSendPing(SendPingEvent e) {}

  protected void onPong(PongEvent e) {}

  protected void onActivate(ActivateEvent e) {}

  protected void onInvalidate(InvalidateEvent e) {}

  protected void onKill(KillEvent e) {}

  protected final void logStartHandleEvent(HeartbeatEvent event) {
    this.logger.debug("[{}][{}]: start handle '{}' event", this.machine.identity(), this, event);
  }

  protected final void transitionTo(HeartbeatState nextState) {
    this.machine.state(nextState);
    this.logger.debug(
        "[{}][{}]: transitioned to '{}' state", this.machine.identity(), this, nextState);
  }

  protected final void completeStop(StopEvent event) {
    CFUtils.linkFuture(machine.rootCloseFuture(), event.closeFuture());
    if (event.cause() != null) {
      this.logger.debug(
          "[{}][{}]: stop policy with cause", machine.identity(), this, event.cause());
    }
    this.machine.rootCloseFuture().complete(null);
  }

  @Override
  public String toString() {
    return this.name;
  }
}

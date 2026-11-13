/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.state.State;

/**
 * The base class of the internal states of the default availability policy. A state works with the
 * machine and with its context without any restrictions.
 */
abstract class PolicyState implements State<PolicyEvent> {

  private final Logger logger;

  final HeartbeatStateMachine machine;
  private final String name;

  PolicyState(HeartbeatStateMachine machine, String name) {
    this.machine = machine;
    this.name = name;
    this.logger = LoggerFactory.getLogger(getClass());
  }

  @Override
  public void handle(PolicyEvent event) {
    if (!isValid(event)) {
      return;
    }

    logReceivedEvent(event);
  }

  boolean isValid(PolicyEvent event) {
    if (event == null) {
      this.logger.debug("[{}][{}]: got a null event. Skipping...", this.machine.identity(), this);
      return false;
    }
    return true;
  }

  protected final void logStartHandleEvent(PolicyEvent event) {
    this.logger.debug("[{}][{}]: start handle '{}' event", this.machine.identity(), this, event);
  }

  protected final void logReceivedEvent(PolicyEvent event) {
    this.logger.debug("[{}][{}]: received '{}' event", this.machine.identity(), this, event);
  }

  protected final void logUnexpectedEvent(PolicyEvent event) {
    this.logger.debug(
        "[{}][{}]: received an unexpected '{}' event", this.machine.identity(), this, event);
  }

  protected final void transitionTo(PolicyState nextState) {
    machine.state(nextState);
    this.logger.debug(
        "[{}][{}]: transitioned to '{}' state", this.machine.identity(), this, nextState);
  }

  @Override
  public String toString() {
    return this.name;
  }
}

/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.StartEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.StopEvent;
import io.tarantool.pool.availability.heartbeat.exceptions.HeartbeatClosedException;

final class StoppedState extends HeartbeatState {

  StoppedState(HeartbeatStateMachine machine) {
    super(machine, "HEARTBEAT_STOPPED");
  }

  @Override
  protected void onStart(StartEvent event) {
    logStartHandleEvent(event);
    event.startFuture().completeExceptionally(new HeartbeatClosedException("Already closed!"));
  }

  @Override
  protected void onStop(StopEvent event) {
    logStartHandleEvent(event);
    completeStop(event);
  }
}

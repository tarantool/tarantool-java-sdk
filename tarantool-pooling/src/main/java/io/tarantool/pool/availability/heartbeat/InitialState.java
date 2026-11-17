/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import io.tarantool.core.utils.CFUtils;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.ActivateEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.StartEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatEvent.StopEvent;
import io.tarantool.pool.availability.heartbeat.HeartbeatStateMachine.HeartbeatCtx;

final class InitialState extends HeartbeatState {

  InitialState(HeartbeatStateMachine machine) {
    super(machine, "HEARTBEAT_NEW");
  }

  @Override
  protected void onStart(StartEvent event) {
    logStartHandleEvent(event);
    final HeartbeatOpts opts = machine.opts();
    this.logger.info(
        "[{}][{}]: start heartbeat with [failures = {}, interval = {}, death pings = {}]",
        this.machine.identity(),
        this,
        opts.getInvalidationThreshold(),
        opts.getPingInterval(),
        opts.getDeathThreshold());

    final HeartbeatCtx ctx = this.machine.ctx();
    ctx.firstSuccess();

    final HeartbeatState nextState = new RunningState(this.machine);
    transitionTo(nextState);
    CFUtils.linkFuture(this.machine.rootStartFuture(), event.startFuture());
    ctx.scheduleNextPing(opts.initialDelay().toMillis());
    nextState.handle(ActivateEvent.INSTANCE);
    this.machine.rootStartFuture().complete(null);
  }

  @Override
  protected void onStop(StopEvent event) {
    logStartHandleEvent(event);
    this.machine.ctx().clear();
    transitionTo(new StoppedState(this.machine));
    this.machine.view().changeState(HeartbeatConstants.INVALIDATE_STATE);
    completeStop(event);
  }
}

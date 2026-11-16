/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import io.tarantool.pool.PoolEntryState;

public final class HeartbeatConstants {

  private HeartbeatConstants() {}

  /** The name of the metric with the count of successful heartbeat ping requests. */
  public static final String POOL_HEARTBEAT_SUCCESS = "pool.heartbeat.success";

  /** The name of the metric with the count of failed heartbeat ping requests. */
  public static final String POOL_HEARTBEAT_ERRORS = "pool.heartbeat.errors";

  /** The name of the metric with the time of the heartbeat ping request. */
  public static final String POOL_HEARTBEAT_TIME = "pool.heartbeat.time";

  public static final String INVALIDATE_STATE_NAME = "HEARTBEAT_INVALIDATE_STATE";
  public static final String KILL_STATE_NAME = "HEARTBEAT_KILL_STATE";
  public static final String ACTIVE_STATE_NAME = "HEARTBEAT_ACTIVE_STATE";

  public static final PoolEntryState INVALIDATE_STATE =
      new PoolEntryState(INVALIDATE_STATE_NAME, true) {};
  public static final PoolEntryState KILL_STATE = new PoolEntryState(KILL_STATE_NAME, true) {};
  public static final PoolEntryState ACTIVE_STATE = new PoolEntryState(ACTIVE_STATE_NAME, false) {};
}

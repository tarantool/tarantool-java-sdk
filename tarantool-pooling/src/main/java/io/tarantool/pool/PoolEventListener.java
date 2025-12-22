/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.pool;

/**
 * Listener for pool connection lifecycle and heartbeat events.
 *
 * <p>The listener is optional. All callbacks are invoked with the tag and index of the affected
 * connection. Default implementations are no-ops so callers may override only needed.
 * Implementations must keep callbacks non-blocking.</p>
 */
public interface PoolEventListener {

  /**
   * Invoked when connection successfully established and ready for use.
   *
   * @param tag connection group tag
   * @param index connection index inside the group
   */
  default void onConnectionOpened(String tag, int index) {}

  /**
   * Invoked when connection closed (either explicitly or due to failures).
   *
   * @param tag connection group tag
   * @param index connection index inside the group
   */
  default void onConnectionClosed(String tag, int index) {}

  /**
   * Invoked when the pool detects a connection failure.
   *
   * @param tag connection group tag
   * @param index connection index inside the group
   * @param throwable failure cause
   */
  default void onConnectionFailed(String tag, int index, Throwable throwable) {}

  /**
   * Invoked when reconnect task scheduled.
   *
   * @param tag connection group tag
   * @param index connection index inside the group
   * @param delayMs reconnect delay in milliseconds
   */
  default void onReconnectScheduled(String tag, int index, long delayMs) {}

  /**
   * Invoked when heartbeat state changes.
   *
   * @param tag connection group tag
   * @param index connection index inside the group
   * @param event heartbeat event
   */
  default void onHeartbeatEvent(String tag, int index, HeartbeatEvent event) {}
}

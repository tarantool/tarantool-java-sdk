/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.pool;

/**
 * <p>Enumeration for heartbeat states.</p>
 * <p>This enumeration class describes heartbeat actions for some connection.</p>
 * <ul>
 *  <li><b>INVALIDATE</b> heartbeat runs this action when connection does not reply to ping requests on time for certain
 *  number of attempts. In this case {@link io.tarantool.pool.PoolEntry} marks itself as unavailable and outer clients
 *  will get a null value as a result obtaining a connection from the pool by tag and index of this entry. </li>
 *  <li><b>ACTIVATE</b> this action means that connection becomes alive and will be returned to outer client requesting
 *  it from the pool by correspondent tag and index. This action is default for any connections initialized in the pool.
 *  Invalidated connection also comes to this state if some count of pings are successful.</li>
 *  <li><b>KILL</b> this action means that an invalidated connection did not respond to ping requests successfully for
 *  some count of attempts and this connection will be closed and reconnected.</li>
 * </ul>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
public enum HeartbeatEvent {
  INVALIDATE,
  ACTIVATE,
  KILL
}

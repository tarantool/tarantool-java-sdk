/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

public enum ConnectionCloseEvent {
  CLOSE_BY_REMOTE,
  CLOSE_BY_CLIENT,
  CLOSE_BY_SHUTDOWN,
  CLOSE_BY_TIMEOUT;
}

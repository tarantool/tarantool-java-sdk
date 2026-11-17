/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat.exceptions;

public class HeartbeatClosedException extends HeartbeatException {

  public HeartbeatClosedException() {
    super();
  }

  public HeartbeatClosedException(String message) {
    super(message);
  }

  public HeartbeatClosedException(String message, Throwable cause) {
    super(message, cause);
  }

  public HeartbeatClosedException(Throwable cause) {
    super(cause);
  }

  protected HeartbeatClosedException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat.exceptions;

public class HeartbeatException extends RuntimeException {

  public HeartbeatException() {
    super();
  }

  public HeartbeatException(String message) {
    super(message);
  }

  public HeartbeatException(String message, Throwable cause) {
    super(message, cause);
  }

  public HeartbeatException(Throwable cause) {
    super(cause);
  }

  protected HeartbeatException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.exceptions;

public enum ApiError {
  FSM_RUN_ONCE_ALREADY_CALLED("IProtoStateMachine.runOnce() is called before, cannot be called again"),
  SHUTDOWN_REQUEST_FINISHED("Request finished by shutdown");

  private final String message;

  ApiError(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}

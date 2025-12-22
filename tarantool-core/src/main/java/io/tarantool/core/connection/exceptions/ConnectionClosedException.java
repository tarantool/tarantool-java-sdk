/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.connection.exceptions;


public class ConnectionClosedException extends ConnectionException {

  private static final long serialVersionUID = -1006437397376111862L;

  public ConnectionClosedException(String message) {
    super(message);
  }

  public ConnectionClosedException(String message, Throwable cause) {
    super(message, cause);
  }
}

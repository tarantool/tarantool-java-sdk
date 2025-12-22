/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.exceptions;

import static io.tarantool.core.exceptions.ApiError.SHUTDOWN_REQUEST_FINISHED;

public class ShutdownException extends ClientException {

  private static final long serialVersionUID = -6855430596832303332L;

  public ShutdownException() {
    super(SHUTDOWN_REQUEST_FINISHED.getMessage());
  }

  public ShutdownException(String message) {
    super(message);
  }
}

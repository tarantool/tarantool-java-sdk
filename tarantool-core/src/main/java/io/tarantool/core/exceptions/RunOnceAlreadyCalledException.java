/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.exceptions;

import static io.tarantool.core.exceptions.ApiError.FSM_RUN_ONCE_ALREADY_CALLED;

public class RunOnceAlreadyCalledException extends RuntimeException {

  public RunOnceAlreadyCalledException() {
    super(FSM_RUN_ONCE_ALREADY_CALLED.getMessage());
  }
}

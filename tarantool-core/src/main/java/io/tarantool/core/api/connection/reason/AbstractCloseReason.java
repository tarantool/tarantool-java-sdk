/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.api.connection.reason;

public abstract class AbstractCloseReason implements CloseReason {

  protected final Throwable cause;

  protected AbstractCloseReason(Throwable cause) {
    this.cause = cause;
  }

  @Override
  public Throwable cause() {
    return this.cause;
  }
}

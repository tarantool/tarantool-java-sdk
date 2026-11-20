/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.api.connection.reason;

/**
 * An implementation of {@link CloseReason}. Represents a reason for closure initiated by remote
 * (network or Tarantool node).
 */
public final class CloseByRemote extends AbstractCloseReason {

  public CloseByRemote() {
    this(null);
  }

  public CloseByRemote(Throwable cause) {
    super(cause);
  }

  @Override
  public boolean byClient() {
    return false;
  }
}

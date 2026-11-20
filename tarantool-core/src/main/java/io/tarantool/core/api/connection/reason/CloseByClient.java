/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.api.connection.reason;

/**
 * An implementation of {@link CloseReason}. Represents a reason for closure initiated by the client
 * or the library.
 */
public final class CloseByClient extends AbstractCloseReason {

  public CloseByClient() {
    this(null);
  }

  public CloseByClient(Throwable cause) {
    super(cause);
  }

  @Override
  public boolean byClient() {
    return true;
  }
}

/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.api.connection.reason;

/** An interface representing the reason for connection closure. */
public interface CloseReason {

  /**
   * The error object. Not null if the connection was closed due to an error.
   *
   * @return may be null if the connection was closed normally.
   */
  Throwable cause();

  /** Returns {@code true} if the connection closure was initiated by the client or the library. */
  boolean byClient();
}

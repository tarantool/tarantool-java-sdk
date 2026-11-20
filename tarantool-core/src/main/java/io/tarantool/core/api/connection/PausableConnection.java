/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.api.connection;

public interface PausableConnection extends Connection {

  /** Stops reading incoming data from the socket. */
  void pause();

  /** Resumes reading incoming data previously stopped by {@link #pause()}. */
  void resume();

  /** Returns whether reading of incoming data is currently paused. */
  boolean isPaused();
}

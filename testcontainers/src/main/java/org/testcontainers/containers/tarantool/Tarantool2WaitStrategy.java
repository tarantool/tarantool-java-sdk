/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool;

import org.testcontainers.containers.wait.strategy.ShellStrategy;

public class Tarantool2WaitStrategy extends ShellStrategy {

  private int port = 3301;

  private static final String FORMAT =
      "if echo \"box.info.status\" | tarantoolctl connect %s:%s@%s:%s | grep -q "
          + "\"running\"; then exit 0; else exit 1; fi";

  public Tarantool2WaitStrategy(CharSequence hostName, CharSequence user, CharSequence password) {
    withCommand(String.format(FORMAT, user, password, hostName, port));
  }

  /** Internal iproto port that Tarantool is listening. */
  Tarantool2WaitStrategy withInternalPort(int port) {
    this.port = port;
    return this;
  }
}

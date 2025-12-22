/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool;

import org.testcontainers.containers.wait.strategy.ShellStrategy;

/**
 * A waiting strategy that checks whether {@code Tarantool} is ready for interaction. Strategy is
 * using {@code tt} to execute {@code box.info.status} command. The passed {@code user} with {@code
 * password} must have permission to execute {@code box.info.status} command.
 */
public class Tarantool3WaitStrategy extends ShellStrategy {

  private int port = 3301;

  private static final String FORMAT =
      "if echo \"box.info.status\" | tt connect %s:%s@%s:%s -x lua | grep -q "
          + "\"running\"; then exit 0; else exit 1; fi";

  public Tarantool3WaitStrategy(CharSequence hostName, CharSequence user, CharSequence password) {
    withCommand(String.format(FORMAT, user, password, hostName, port));
  }

  /** Internal iproto port that Tarantool is listening. */
  Tarantool3WaitStrategy withInternalPort(int port) {
    this.port = port;
    return this;
  }
}

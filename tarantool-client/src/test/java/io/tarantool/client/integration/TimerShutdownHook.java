/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.integration;

import io.netty.util.Timer;

public final class TimerShutdownHook {
  private TimerShutdownHook() {}

  public static void register(Timer timer, String name) {
    Runtime.getRuntime().addShutdownHook(new Thread(timer::stop, name));
  }
}

/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.integration;

import java.util.Set;
import java.util.function.Supplier;

import io.netty.util.Timeout;
import io.netty.util.Timer;

public final class TimerShutdownHook {
  private TimerShutdownHook() {}

  public static void register(Supplier<Timer> timerRef, String name) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  Timer t = timerRef.get();
                  if (t != null) {
                    Set<Timeout> pending = t.stop();
                    if (pending != null) pending.forEach(Timeout::cancel);
                  }
                },
                name));
  }
}

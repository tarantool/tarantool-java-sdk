/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.support;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

/** A timer that fails the given count of the first timeouts and delegates the rest. */
public class FlakyTimer implements Timer {

  private final Timer delegate;
  private final AtomicInteger failuresLeft;

  public FlakyTimer(Timer delegate, int failures) {
    this.delegate = delegate;
    this.failuresLeft = new AtomicInteger(failures);
  }

  @Override
  public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
    if (this.failuresLeft.getAndDecrement() > 0) {
      throw new RuntimeException("the timer is broken");
    }
    return this.delegate.newTimeout(task, delay, unit);
  }

  @Override
  public Set<Timeout> stop() {
    return this.delegate.stop();
  }
}

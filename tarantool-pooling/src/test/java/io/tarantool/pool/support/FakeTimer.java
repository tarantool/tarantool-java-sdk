/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

/**
 * A {@link Timer} that fires nothing by itself: the test decides when a scheduled timeout runs.
 *
 * <p>The heartbeat policy schedules the next ping through the timer it has been configured with, so
 * a manual timer turns the whole heartbeat into a deterministic sequence of steps: the test fires a
 * timeout, the policy sends a ping, the test completes the ping future, and the policy reacts to
 * the answer. Neither sleeps nor polling are needed.
 *
 * <p>The timeouts are fired in the order of their scheduling. A timeout scheduled by a fired task
 * is not fired by the same {@link #fireAll()} call, so the test may walk through the heartbeat step
 * by step.
 */
public final class FakeTimer implements Timer {

  private final ConcurrentLinkedQueue<FakeTimeout> pending = new ConcurrentLinkedQueue<>();
  private final AtomicInteger scheduled = new AtomicInteger();
  private final AtomicInteger fired = new AtomicInteger();
  private final AtomicInteger cancelled = new AtomicInteger();
  private volatile FakeTimeout lastScheduled;

  @Override
  public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
    final FakeTimeout timeout = new FakeTimeout(this, task, unit.toMillis(delay));
    this.pending.add(timeout);
    this.scheduled.incrementAndGet();
    this.lastScheduled = timeout;
    return timeout;
  }

  @Override
  public Set<Timeout> stop() {
    final Set<Timeout> stopped = new HashSet<>();
    for (final FakeTimeout timeout : pendingTimeouts()) {
      if (timeout.cancel()) {
        stopped.add(timeout);
      }
    }
    this.pending.clear();
    return stopped;
  }

  /**
   * Returns the timeouts that are scheduled but neither fired nor cancelled.
   *
   * @return the immutable snapshot of the pending timeouts
   */
  public List<FakeTimeout> pendingTimeouts() {
    return Collections.unmodifiableList(new ArrayList<>(this.pending));
  }

  /**
   * Returns the count of the timeouts that are scheduled but neither fired nor cancelled.
   *
   * @return the count of the pending timeouts
   */
  public int pendingCount() {
    return this.pending.size();
  }

  /**
   * Returns the count of all the scheduled timeouts.
   *
   * @return the count of the scheduled timeouts
   */
  public int scheduledCount() {
    return this.scheduled.get();
  }

  /**
   * Returns the count of the fired timeouts.
   *
   * @return the count of the fired timeouts
   */
  public int firedCount() {
    return this.fired.get();
  }

  /**
   * Returns the count of the cancelled timeouts.
   *
   * @return the count of the cancelled timeouts
   */
  public int cancelledCount() {
    return this.cancelled.get();
  }

  /**
   * Returns the timeout that has been scheduled last, no matter whether it is fired or not.
   *
   * @return the last scheduled timeout or {@code null} if nothing has been scheduled
   */
  public FakeTimeout lastScheduled() {
    return this.lastScheduled;
  }

  /**
   * Fires the timeout that has been scheduled first.
   *
   * @return the fired timeout or {@code null} if there is nothing to fire
   */
  public FakeTimeout fireNext() {
    final FakeTimeout timeout = this.pending.poll();
    if (timeout == null) {
      return null;
    }
    timeout.fire();
    return timeout;
  }

  /**
   * Fires all the timeouts that are pending at the moment of the call.
   *
   * <p>The timeouts scheduled by the fired tasks stay pending: the test may fire them one by one to
   * control the heartbeat step by step.
   *
   * @return the count of the fired timeouts
   */
  public int fireAll() {
    int count = 0;
    for (final FakeTimeout timeout : pendingTimeouts()) {
      if (timeout.fire()) {
        count++;
      }
    }
    return count;
  }

  /** A timeout scheduled in a {@link FakeTimer}. */
  public static final class FakeTimeout implements Timeout {

    private final FakeTimer timer;
    private final TimerTask task;
    private final long delayMillis;
    private final AtomicBoolean expired = new AtomicBoolean();
    private final AtomicBoolean cancelled = new AtomicBoolean();

    private FakeTimeout(FakeTimer timer, TimerTask task, long delayMillis) {
      this.timer = timer;
      this.task = task;
      this.delayMillis = delayMillis;
    }

    /**
     * Returns the delay of the timeout in milliseconds.
     *
     * @return the delay in milliseconds
     */
    public long delayMillis() {
      return this.delayMillis;
    }

    /**
     * Runs the task of the timeout.
     *
     * @return {@code true} if the task has been run, {@code false} if the timeout has been fired or
     *     cancelled before
     */
    public boolean fire() {
      if (!this.expired.compareAndSet(false, true)) {
        return false;
      }
      this.timer.pending.remove(this);
      this.timer.fired.incrementAndGet();
      try {
        this.task.run(this);
      } catch (Exception e) {
        throw new IllegalStateException("The timer task has failed", e);
      }
      return true;
    }

    @Override
    public Timer timer() {
      return this.timer;
    }

    @Override
    public TimerTask task() {
      return this.task;
    }

    @Override
    public boolean isExpired() {
      return this.expired.get();
    }

    @Override
    public boolean isCancelled() {
      return this.cancelled.get();
    }

    @Override
    public boolean cancel() {
      if (!this.cancelled.compareAndSet(false, true)) {
        return false;
      }
      this.timer.pending.remove(this);
      this.timer.cancelled.incrementAndGet();
      return true;
    }

    @Override
    public String toString() {
      return "FakeTimeout{delayMillis="
          + this.delayMillis
          + ", expired="
          + this.expired.get()
          + ", cancelled="
          + this.cancelled.get()
          + '}';
    }
  }
}

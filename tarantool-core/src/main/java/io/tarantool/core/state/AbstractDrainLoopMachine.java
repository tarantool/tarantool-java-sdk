/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.state;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDrainLoopMachine<E extends Event> implements StateMachine<E> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDrainLoopMachine.class);

  private final Queue<E> controlEvents = new ConcurrentLinkedQueue<>();
  private final Queue<E> events = new ConcurrentLinkedQueue<>();
  private final AtomicInteger wip = new AtomicInteger(0);

  @Override
  public void handle(E event) {
    if (event == null) {
      return;
    }

    if (event.isControl()) {
      this.controlEvents.offer(event);
    } else {
      this.events.offer(event);
    }

    if (this.wip.getAndIncrement() != 0) {
      return;
    }

    int missed = 1;
    try {
      do {
        E e;

        while ((e = this.controlEvents.poll()) != null) {
          safelyProcess(e);
        }

        if ((e = this.events.poll()) != null) {
          safelyProcess(e);
          // go to check control events again
          continue;
        }

        missed = this.wip.addAndGet(-missed);
      } while (missed != 0);
    } catch (Throwable criticalJvmError) {
      this.controlEvents.clear();
      this.events.clear();
      this.wip.set(0);
      throw criticalJvmError;
    }
  }

  private void safelyProcess(E e) {
    try {
      processEvent(e);
    } catch (Exception ex) {
      LOGGER.error("[{}]: unexpected error in event handler. Try handle error...", this, ex);
      try {
        onHandleError(ex);
      } catch (Exception failSafeEx) {
        LOGGER.error("[{}]: fail-safe handler failed", this, failSafeEx);
      }
    }
  }

  protected abstract void processEvent(E event);

  protected abstract void onHandleError(Throwable error);
}

/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.state;

import java.util.concurrent.RejectedExecutionException;

import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNettyMachine<E extends Event> implements StateMachine<E> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNettyMachine.class);

  protected final EventLoop loop;

  protected AbstractNettyMachine(EventLoop loop) {
    this.loop = loop;
  }

  @Override
  public void handle(E event) {
    if (this.loop.inEventLoop()) {
      internalHandle(event);
    } else {
      try {
        this.loop.execute(() -> internalHandle(event));
      } catch (RejectedExecutionException ex) {
        LOGGER.error(
            "[{}]: catch an unexpected machine event execution. Try handle this...", this, ex);
        try {
          onHandleRejectedException(event, ex);
        } catch (Throwable failSafeEx) {
          LOGGER.error("[{}]: fail-safe handler failed", this, failSafeEx);
        }
      }
    }
  }

  private void internalHandle(E event) {
    try {
      processEvent(event);
    } catch (Throwable error) {
      try {
        LOGGER.error("[{}]: unexpected error in event handler. Try handle error...", this, error);
        onHandleError(error);
      } catch (Throwable failSafeEx) {
        LOGGER.error("[{}]: fail-safe handler failed", this, failSafeEx);
      }
    }
  }

  protected abstract void processEvent(E event);

  protected abstract void onHandleError(Throwable error);

  protected abstract void onHandleRejectedException(E event, RejectedExecutionException e);
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.fsm;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.WatcherOptions;
import io.tarantool.core.connection.Connection;
import io.tarantool.core.exceptions.ClientException;
import io.tarantool.core.protocol.IProtoRequest;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.protocol.requests.IProtoWatch;

public class WatcherStateMachine implements IProtoStateMachine {

  private final Connection connection;

  private static final Logger log = LoggerFactory.getLogger(WatcherStateMachine.class);

  private final IProtoRequest request;

  private final String key;

  private final Consumer<IProtoResponse> callback;

  private final Timer timerService;

  private final WatcherOptions opts;

  private boolean calledOnce;

  public WatcherStateMachine(
      String key,
      long syncId,
      Consumer<IProtoResponse> callback,
      Connection connection,
      WatcherOptions opts,
      Timer timerService) {
    this.key = key;
    this.connection = connection;
    this.callback = callback;
    this.request = new IProtoWatch(key);
    this.request.setSyncId(syncId);
    this.opts = opts;
    this.timerService = timerService;
  }

  @Override
  public void runOnce() {
    if (calledOnce) {
      throw new IllegalStateException("runOnce() is called before, cannot be called again.");
    }
    calledOnce = true;
    send();
  }

  /**
   * A function that handles the message of an event triggered in Tarantool using the suggested
   * callbacks.
   *
   * <p>TODO: now we always use "return false". If it will change, this function should be
   * corrected.
   *
   * <p>TODO: add handling for errors and logging.
   *
   * @param message message sent from Tarantool as a result of watch event triggering.
   */
  @Override
  public boolean process(IProtoResponse message) {
    log.debug("WatcherStateMachine:process() - \"{}\"", message);

    if (!message.isError()) {
      callback.accept(message);
      // Used to avoid sending a response to the box.shutdown event during a
      // graceful shutdown.
      if (connection.isConnected()) {
        send();
      }
    } else {
      log.warn("got error for watcher: {}", message);
      opts.getErrorHandler().accept(key, new ClientException("watcher error: %s", message));
    }

    return false;
  }

  @Override
  public void kill(Throwable exc) {}

  private void onSendComplete(Void r, Throwable exc) {
    if (exc != null) {
      log.warn("could not send IPROTO_WATCH packet: %s", exc);
    } else {
      log.debug("IPROTO_WATCH sent");
    }
  }

  private void send() {
    CompletableFuture<Void> future = connection.send(request).whenComplete(this::onSendComplete);
    Timeout timer =
        timerService.newTimeout(
            timeoutHandler -> {
              if (!future.isDone()) {
                future.completeExceptionally(new ClientException("watcher timeout"));
              }
            },
            opts.getSendTimeout(),
            TimeUnit.MILLISECONDS);
    future.whenComplete((r, exc) -> timer.cancel());
  }
}

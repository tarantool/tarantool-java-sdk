/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.fsm;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.connection.Connection;
import io.tarantool.core.exceptions.BoxError;
import io.tarantool.core.protocol.IProtoMessage;
import io.tarantool.core.protocol.IProtoRequest;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;

public class RequestStateMachine extends AbstractIProtoStateMachine {

  private static final Logger log = LoggerFactory.getLogger(RequestStateMachine.class);
  private final Consumer<IProtoMessage> pushConsumer;
  private final Timer timerService;
  private final long timeout;
  private final CompletableFuture<IProtoResponse> promise;
  private Timeout timer;

  public RequestStateMachine(
      Connection connection,
      long syncId,
      IProtoRequest request,
      CompletableFuture<IProtoResponse> promise,
      IProtoRequestOpts opts,
      Map<Long, IProtoStateMachine> fsmRegistry,
      Timer timerService) {
    super(connection, request, fsmRegistry);
    request.setSyncId(syncId);
    this.pushConsumer = opts.getPushHandler();
    this.timerService = timerService;
    this.timeout = opts.getRequestTimeout();
    this.promise = promise;
    fsmRegistry.put(syncId, this);
  }

  @Override
  public void start() {
    // TODO: remove string format
    this.timer =
        runAfter(
            timeout,
            () ->
                kill(
                    new TimeoutException(
                        String.format("Request timeout: %s; timeout = %sms", request, timeout))));

    promise.whenComplete((iProtoResponse, throwable) -> this.timer.cancel());

    connection
        .send(request)
        .handle(
            (Void r, Throwable ex) -> {
              if (ex != null) {
                log.debug(ex.toString(), ex);
                fsmRegistry.remove(request.getSyncId());
                promise.completeExceptionally(ex);
              }
              return null;
            });
  }

  @Override
  public boolean process(IProtoResponse message) {
    if (message.isOutOfBand()) {
      if (pushConsumer != null) {
        pushConsumer.accept(message);
      } else {
        log.debug("Can not process message \"{}\" because pushConsumer is empty", message);
      }
      return false;
    }

    if (message.isError()) {
      promise.completeExceptionally(BoxError.fromIProtoMessage(message));
    } else {
      promise.complete(message);
    }

    return true;
  }

  @Override
  public void kill(Throwable ex) {
    fsmRegistry.remove(request.getSyncId());
    promise.completeExceptionally(ex);
  }

  private Timeout runAfter(long after, Runnable cb) {
    if (after == 0) {
      cb.run();
      return null;
    }

    return timerService.newTimeout(timeoutHandler -> cb.run(), after, TimeUnit.MILLISECONDS);
  }
}

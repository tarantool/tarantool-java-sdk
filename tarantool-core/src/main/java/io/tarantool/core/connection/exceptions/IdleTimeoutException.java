/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection.exceptions;

import io.netty.channel.ChannelException;
import io.netty.handler.timeout.IdleStateEvent;

public final class IdleTimeoutException extends ChannelException {

  private final IdleStateEvent event;

  public IdleTimeoutException(String message, IdleStateEvent event) {
    super(message);
    this.event = event;
  }

  public IdleStateEvent getEvent() {
    return event;
  }
}

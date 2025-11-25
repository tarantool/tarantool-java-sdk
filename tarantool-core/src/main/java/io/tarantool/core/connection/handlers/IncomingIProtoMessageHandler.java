/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection.handlers;

import java.util.function.BiConsumer;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

import io.tarantool.core.connection.exceptions.IdleTimeoutException;
import io.tarantool.core.protocol.IProtoResponse;

/**
 * Basic Tarantool server response handler. Dispatches incoming message either to an error or a
 * normal result handler.
 *
 * @author Ivan Bannikov
 */
public class IncomingIProtoMessageHandler extends SimpleChannelInboundHandler<IProtoResponse> {

  private final BiConsumer<IProtoResponse, Throwable> messageHandler;

  public IncomingIProtoMessageHandler(BiConsumer<IProtoResponse, Throwable> handler) {
    super();
    this.messageHandler = handler;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, IProtoResponse msg) {
    this.messageHandler.accept(msg, null);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
    ChannelConfig cfg = ctx.channel().config();
    if (cfg.isAutoRead() && event instanceof IdleStateEvent) {
      messageHandler.accept(null, new IdleTimeoutException("timeout", (IdleStateEvent) event));
    }
  }
}

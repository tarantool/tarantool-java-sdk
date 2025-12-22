/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection.handlers;

import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import io.tarantool.core.connection.Greeting;

public class GreetingHandler extends SimpleChannelInboundHandler<ByteBuf> {

  private static final int GREETING_LENGTH = 128;

  private final CompletableFuture<Greeting> promise;

  public GreetingHandler(CompletableFuture<Greeting> promise) {
    this.promise = promise;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
    if (in.readableBytes() < GREETING_LENGTH) {
      return;
    }
    byte[] greeting = new byte[GREETING_LENGTH];
    in.readBytes(greeting);
    this.promise.complete(Greeting.parse(greeting));
    ctx.pipeline().remove(this);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    promise.completeExceptionally(cause);
  }
}

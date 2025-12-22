/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

import io.tarantool.core.connection.codecs.IProtoFrameDecoder;
import io.tarantool.core.connection.codecs.IProtoFrameEncoder;
import io.tarantool.core.connection.handlers.GreetingHandler;
import io.tarantool.core.connection.handlers.IncomingIProtoMessageHandler;
import io.tarantool.core.connection.handlers.OutgoingIProtoMessageHandler;
import io.tarantool.core.protocol.IProtoResponse;


class ConnectionChannelInitializer extends ChannelInitializer<SocketChannel> {

  private final CompletableFuture<Greeting> promise;
  private final BiConsumer<IProtoResponse, Throwable> messageHandler;
  private final SslContext sslContext;
  private final ChannelFutureListener closeHandler;
  private final FlushConsolidationHandler flushConsolidationHandler;
  private final int idleTimeout;

  private ConnectionChannelInitializer(CompletableFuture<Greeting> promise,
      BiConsumer<IProtoResponse, Throwable> messageHandler,
      SslContext sslContext,
      ChannelFutureListener closeHandler,
      FlushConsolidationHandler flushConsolidationHandler,
      int idleTimeout) {
    this.promise = promise;
    this.messageHandler = messageHandler;
    this.sslContext = sslContext;
    this.closeHandler = closeHandler;
    this.flushConsolidationHandler = flushConsolidationHandler;
    this.idleTimeout = idleTimeout;
  }

  @Override
  protected void initChannel(SocketChannel socketChannel) {
    ChannelPipeline pipeline = socketChannel.pipeline();
    socketChannel.closeFuture().addListener(closeHandler);

    if (flushConsolidationHandler != null) {
      pipeline.addLast("FlushConsolidationHandler", flushConsolidationHandler);
    }

    if (this.sslContext != null) {
      pipeline.addLast(this.sslContext.newHandler(socketChannel.alloc()));
    }

    if (this.idleTimeout > 0) {
      pipeline.addLast(
          "IdleTimeoutHandler",
          new IdleStateHandler(false, idleTimeout, 0, 0, TimeUnit.MILLISECONDS)
      );
    }

    pipeline.addLast(
        "GreetingHandler",
        new GreetingHandler(promise)
    );
    pipeline.addLast(
        "MessagePackFrameDecoder",
        new IProtoFrameDecoder()
    );
    pipeline.addLast(
        "MessagePackFrameEncoder",
        new IProtoFrameEncoder()
    );
    pipeline.addLast(
        "OutgoingIProtoMessageHandler",
        new OutgoingIProtoMessageHandler()
    );
    pipeline.addLast(
        "IncomingIProtoMessageHandler",
        new IncomingIProtoMessageHandler(messageHandler)
    );
  }

  public static class Builder {

    private CompletableFuture<Greeting> promise;
    private BiConsumer<IProtoResponse, Throwable> messageHandler;
    private SslContext sslContext = null;
    private ChannelFutureListener closeHandler;
    private FlushConsolidationHandler flushConsolidationHandler = null;
    private int idleTimeout = -1;

    public Builder() {}

    public Builder withConnectPromise(CompletableFuture<Greeting> promise) {
      this.promise = promise;
      return this;
    }

    public Builder withMessageHandler(BiConsumer<IProtoResponse, Throwable> handler) {
      this.messageHandler = handler;
      return this;
    }

    public Builder withSSLContext(SslContext sslContext) {
      this.sslContext = sslContext;
      return this;
    }

    public Builder withCloseHandler(ChannelFutureListener listener) {
      this.closeHandler = listener;
      return this;
    }

    public Builder withFlushConsolidationHandler(FlushConsolidationHandler flushConsolidationHandler) {
      this.flushConsolidationHandler = flushConsolidationHandler;
      return this;
    }

    public Builder withIdleTimeout(int idleTimeout) {
      this.idleTimeout = idleTimeout;
      return this;
    }

    ConnectionChannelInitializer build() {
      return new ConnectionChannelInitializer(
          this.promise,
          this.messageHandler,
          this.sslContext,
          this.closeHandler,
          this.flushConsolidationHandler,
          this.idleTimeout
      );
    }
  }
}

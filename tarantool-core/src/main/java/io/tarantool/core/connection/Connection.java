/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.tarantool.core.protocol.IProtoMessage;
import io.tarantool.core.protocol.IProtoResponse;

public interface Connection extends AutoCloseable {

  /**
   * @deprecated Use {@link #connect(InetSocketAddress, Duration)}
   */
  @Deprecated
  CompletableFuture<Greeting> connect(InetSocketAddress addr, long timeoutMs)
      throws IllegalStateException;

  CompletionStage<Greeting> connect(InetSocketAddress address, Duration timeout)
      throws IllegalStateException;

  CompletableFuture<Void> send(IProtoMessage message) throws IllegalStateException;

  Connection listen(Consumer<IProtoResponse> listener);

  Connection onClose(ConnectionCloseEvent event, BiConsumer<Connection, Throwable> handler);

  Optional<Greeting> getGreeting();

  boolean isConnected();

  void shutdownClose();

  void pause();

  void resume();

  /**
   * @deprecated Use {@link #setIdleTimeout(Duration)}
   */
  @Deprecated
  void setIdleTimeout(int idleTimeout);

  void setIdleTimeout(Duration timeout);

  boolean isPaused();
}

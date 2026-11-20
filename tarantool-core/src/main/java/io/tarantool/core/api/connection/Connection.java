/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.api.connection;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.tarantool.core.api.connection.reason.CloseReason;
import io.tarantool.core.connection.Greeting;
import io.tarantool.core.protocol.IProtoMessage;
import io.tarantool.core.protocol.IProtoResponse;

/**
 * Tarantool node connection interface.
 *
 * @implSpec All implementations must be single-use (one-shot). This means that once the connection
 *     lifecycle has ended, it is not possible to reconnect using the same {@link Connection}
 *     object.
 */
public interface Connection extends AutoCloseable {

  /**
   * Connects to a Tarantool node asynchronously.
   *
   * @return a connection promise represented as a {@link CompletionStage} object. Completes
   *     successfully when the connection to the node is actually established, or exceptionally if
   *     the connection fails for any reason. Never null.
   * @implSpec the implementation must be thread-safe and idempotent. Throughout the connection
   *     lifecycle, only the first connection request must initiate the connection; all subsequent
   *     connection requests must return the result of the first request.
   */
  CompletionStage<Greeting> connect();

  /**
   * Sends a message to a Tarantool node asynchronously.
   *
   * @param message the message, represented as an {@link IProtoMessage} object. Must not be null.
   * @return a {@link CompletionStage} object representing the message delivery result. Completes
   *     successfully if the message is sent to the Tarantool node, or exceptionally if the message
   *     fails to send (for example, if the connection is not established or already closed). Never
   *     null.
   * @throws NullPointerException if {@code message} is null.
   */
  CompletionStage<Void> send(IProtoMessage message);

  /**
   * Returns whether the connection is in a connected state at the time of invocation.
   *
   * @return {@code true} if the connection is in a connected state at the moment this method
   *     executes.
   */
  boolean isConnected();

  /**
   * Asynchronously closes the connection to a Tarantool node. Must not throw exceptions.
   *
   * @return a {@link CompletionStage} that completes successfully if the connection is closed
   *     successfully, or completes exceptionally if any error occurs during closing. Never null.
   * @implSpec to avoid waiting indefinitely for closure, if the connection has not been
   *     successfully closed upon expiration of {@link #closeTimeout()}, the {@link CompletionStage}
   *     must complete exceptionally with a {@link java.util.concurrent.TimeoutException}. The
   *     implementation must be thread-safe and idempotent across both {@link #closeAsync()} and
   *     {@link #shutdownCloseAsync()}: only the first close request must initiate the closure, and
   *     all subsequent close requests must return the result of the first request.
   */
  CompletionStage<Void> closeAsync();

  /** Synchronous version of {@link #closeAsync()}. */
  @Override
  default void close() throws Exception {
    closeAsync().toCompletableFuture().get(closeTimeout().toNanos(), TimeUnit.NANOSECONDS);
  }

  /**
   * Initiates closure of the connection asynchronously, reporting the close cause as a graceful
   * shutdown of the server.
   *
   * <p>This method is intended to be called upon receiving the {@code box.shutdown} notification
   * from Tarantool. At this moment, the connection is still operational, but the server is
   * preparing to stop.
   *
   * <p>In contrast to {@link #close()}, the close is reported to the close handler as a server
   * shutdown reason rather than a client-initiated closure.
   *
   * @return a {@link CompletionStage} that completes successfully if the connection is closed
   *     successfully, or completes exceptionally if any error occurs during closing. Never null.
   * @implSpec obeys the same timeout, thread-safety, and idempotency guarantees as {@link
   *     #closeAsync()}.
   * @see <a href="https://tarantool.io">Tarantool documentation</a>
   */
  CompletionStage<Void> shutdownCloseAsync();

  /** Synchronous version of {@link #shutdownCloseAsync()}. */
  default void shutdownClose() throws Exception {
    shutdownCloseAsync().toCompletableFuture().get(closeTimeout().toNanos(), TimeUnit.NANOSECONDS);
  }

  /**
   * Returns the {@link InetSocketAddress} object to which this {@link Connection} connects.
   *
   * @return never null.
   */
  InetSocketAddress address();

  /**
   * Returns the connection timeout.
   *
   * @return never null.
   */
  Duration connectTimeout();

  /**
   * Returns the connection close timeout.
   *
   * @return never null.
   */
  Duration closeTimeout();

  /**
   * Returns onClose handler.
   *
   * @return never null.
   */
  BiConsumer<Connection, CloseReason> onCloseHandler();

  /**
   * Returns responseHandler.
   *
   * @return never null.
   */
  Consumer<IProtoResponse> responseHandler();
}

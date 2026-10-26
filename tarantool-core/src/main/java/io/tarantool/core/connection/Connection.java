/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.tarantool.core.protocol.IProtoMessage;
import io.tarantool.core.protocol.IProtoResponse;

/**
 * Tarantool node connection interface.
 *
 * @implSpec if an implementation of this interface represents a connection that allows reconnecting
 *     via the same {@link Connection} object, the implementation must document this behavior.
 */
public interface Connection extends AutoCloseable {

  /**
   * Connects to a Tarantool node asynchronously.
   *
   * @param addr the Tarantool node address, represented as an {@link InetSocketAddress} object.
   *     Must not be null.
   * @param timeoutMs the time from the start of the connection attempt until the actual connection
   *     is established. If the connection to the Tarantool node is not established after this time
   *     expires, the connection transitions to an unconnected state, and the {@link
   *     CompletableFuture} promise completes exceptionally.
   * @return a connection promise represented as a {@link CompletableFuture} object. Completes
   *     successfully when the connection to the node is actually established, or exceptionally if
   *     the connection fails for any reason. Never null.
   * @throws IllegalStateException if a connection attempt is made in a state where connecting is
   *     prohibited. The states in which connecting is prohibited are defined by the implementation.
   * @throws IllegalArgumentException if {@code timeoutMs <= 0}.
   */
  CompletableFuture<Greeting> connect(InetSocketAddress addr, long timeoutMs)
      throws IllegalStateException;

  /**
   * Sends a message to a Tarantool node asynchronously.
   *
   * @param message the message, represented as an {@link IProtoMessage} object. Must not be null.
   * @return a {@link CompletableFuture} object representing the message delivery result. Completes
   *     successfully if the message is sent to the Tarantool node, or exceptionally if the message
   *     fails to send. Never null.
   * @throws IllegalStateException if, at the time of sending, the connection is not in a
   *     ready-to-send state.
   */
  CompletableFuture<Void> send(IProtoMessage message) throws IllegalStateException;

  /**
   * Registers a callback for handling any messages from a Tarantool node, which are represented as
   * an {@link IProtoMessage} object.
   *
   * @param listener the callback for handling any messages from the Tarantool node. Must not be
   *     null.
   * @return the same {@link Connection} object on which this method was invoked.
   * @implSpec any implementation-specific details of this method - such as overwriting the previous
   *     callback upon multiple invocations, or other distinct behaviors - must be documented in the
   *     method's implementation documentation.
   */
  Connection listen(Consumer<IProtoResponse> listener);

  /**
   * Registers a callback to receive notifications when the connection is closed for any reason, as
   * reflected by a {@link ConnectionCloseEvent}.
   *
   * @param event the event that caused the connection to close.
   * @param handler the connection close event handler.
   * @return the same connection object on which this method was invoked.
   * @apiNote when the connection is closed for any reason, only the callbacks that were subscribed
   *     to the corresponding event will be invoked.
   * @implSpec implementations must guarantee that the connection is in a closed state at the moment
   *     the callback is invoked.
   */
  Connection onClose(ConnectionCloseEvent event, BiConsumer<Connection, Throwable> handler);

  /**
   * Returns the {@link Greeting} object.
   *
   * @return {@link Optional#empty()} if the connection is not in a connected state.
   */
  Optional<Greeting> getGreeting();

  /**
   * Returns whether the connection is in a connected state at the time of invocation.
   *
   * @return {@code true} if the connection is in a connected state at the moment this method
   *     executes.
   */
  boolean isConnected();

  /**
   * Initiates connection closure of the connection asynchronously, reporting the close cause as a
   * graceful shutdown of the server.
   *
   * <p>This method is intended to be called upon receiving the {@code box.shutdown} notification
   * from Tarantool. At this moment, the connection is still operational, but the server is
   * preparing to stop.
   *
   * <p>In contrast to {@link #close()}, the close is reported to listeners as {@link
   * ConnectionCloseEvent#CLOSE_BY_SHUTDOWN} rather than {@link
   * ConnectionCloseEvent#CLOSE_BY_CLIENT}. Their reaction to these events is expected to differ.
   *
   * @see <a
   *     href="https://www.tarantool.io/ru/doc/latest/reference/internals/iproto/graceful_shutdown/">Tarantool
   *     documentation</a>
   */
  void shutdownClose();

  /** Initiates connection closure via a {@link ConnectionCloseEvent} asynchronously. */
  @Override
  void close() throws Exception;

  /** Sets the idle timeout duration in milliseconds. */
  void setIdleTimeout(int idleTimeoutMs);

  /** Stops reading incoming data from the socket. */
  void pause();

  /** Resumes reading incoming data previously stopped by {@link #pause()}. */
  void resume();

  /** Returns whether reading of incoming data is currently paused. */
  boolean isPaused();
}

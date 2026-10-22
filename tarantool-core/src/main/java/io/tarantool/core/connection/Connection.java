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

/** An interface representing interaction with a Tarantool node via a connection abstraction. */
public interface Connection extends AutoCloseable {

  /**
   * Method for asynchronous connection to a Tarantool node.
   *
   * @param addr address at which the connection is being established
   * @param timeoutMs timeout, specified in milliseconds, that defines the maximum wait time between
   *     the start of a connection attempt and the receipt of a greeting packet
   * @return an instance of {@link CompletableFuture} that completes with a greeting or an error.
   */
  CompletableFuture<Greeting> connect(InetSocketAddress addr, long timeoutMs)
      throws IllegalStateException;

  /**
   * Asynchronously sends an {@link IProtoMessage} object to a Tarantool node.
   *
   * @param message the message to be sent to Tarantool.
   * @return a {@link CompletableFuture} object.
   * @throws IllegalStateException if the method is called when the connection is not in a state
   *     ready to send or receive messages.
   */
  CompletableFuture<Void> send(IProtoMessage message) throws IllegalStateException;

  /**
   * Registers a callback that will receive any {@link IProtoResponse} message arriving from a
   * Tarantool node.
   *
   * @param listener A handler for responses from Tarantool. Must not be {@code null}.
   * @return The same connection object on which this method was called.
   * @implSpec Each subsequent call to this method will overwrite the previously registered handler.
   *     Not thread-safe.
   * @apiNote Typically used when connection object are creating and not used thereafter.
   */
  Connection listen(Consumer<IProtoResponse> listener);

  /**
   * A method that registers a callback to handle a connection-close event.
   *
   * @param event the connection-close event ({@link ConnectionCloseEvent}) for which the handler is
   *     being registered.
   * @param handler the handler object.
   * @implSpec If multiple handlers are provided for the same event, the implementation must
   *     register all of them, rather than replacing a previously registered handler with a new one.
   *     All callbacks are guaranteed to be invoked after the connection has transitioned to the
   *     closed state.
   * @apiNote Typically used during the creation of a connection object and not used thereafter.
   */
  Connection onClose(ConnectionCloseEvent event, BiConsumer<Connection, Throwable> handler);

  /**
   * Gets greeting packet.
   *
   * @return {@link Optional#empty()} if this method was called before the connection reached a
   *     state ready.
   */
  Optional<Greeting> getGreeting();

  /**
   * Checks whether the connection is currently active and ready to process requests.
   *
   * <p>Note that this status represents a snapshot in time. The connection state may change
   * immediately after this method returns.
   *
   * @return {@code true} if the connection is currently in the ready state; {@code false} otherwise
   */
  boolean isConnected();

  /**
   * Closes the connection reporting the close cause as a graceful shutdown of the server.
   *
   * <p>The method is meant to be called upon receiving the {@code box.shutdown} notification from
   * Tarantool. At that moment the connection is still operational, it is the server that is about
   * to terminate.
   *
   * <p>In contrast to {@link #close()}, the close is reported to the listeners as {@link
   * ConnectionCloseEvent#CLOSE_BY_SHUTDOWN} rather than {@link
   * ConnectionCloseEvent#CLOSE_BY_CLIENT}, and they are expected to react differently: a connection
   * closed by the client needs no attention, while a connection closed by shutdown has to be
   * replaced with a new one. The method itself does not reestablish the connection.
   *
   * <p>Requests pending on the connection are not failed by this method, the caller is responsible
   * for cancelling them. The method does nothing if the connection is already closed or is being
   * closed.
   *
   * @see <a
   *     href="https://www.tarantool.io/ru/doc/latest/reference/internals/iproto/graceful_shutdown/">Tarantool
   *     graceful shutdown</a>
   */
  void shutdownClose();

  void setIdleTimeout(int idleTimeout);

  /**
   * Method that pauses the reading of the buffer containing incoming data at the OS level for
   * Netty.
   */
  void pause();

  /**
   * Method that resumes the reading of the buffer containing incoming data at the OS level for
   * Netty.
   */
  void resume();

  /**
   * A method that checks whether automatic reading of a buffer with incoming data has been stopped
   * at the OS level for Netty.
   */
  boolean isPaused();
}

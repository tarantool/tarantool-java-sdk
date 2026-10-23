/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

/** {@link Connection} close events. */
public enum ConnectionCloseEvent {

  /**
   * This event reflects a situation where the connection is closed from the network side—for
   * example, due to a network disconnection.
   */
  CLOSE_BY_REMOTE,

  /**
   * An event reflecting a situation where the connection has been closed by the client-side calling
   * code.
   */
  CLOSE_BY_CLIENT,

  /**
   * This event reflects a situation where a connection is closed from the Tarantool side via a call
   * to {@code box.shutdown}. Unlike {@link #CLOSE_BY_REMOTE}, this event does not indicate network
   * issues; rather, it signifies that a message has been received from a Tarantool node initiating
   * a graceful shutdown.
   *
   * @see <a
   *     href="https://www.tarantool.io/ru/doc/latest/reference/internals/iproto/graceful_shutdown/">Tarantool
   *     documentation</a>
   */
  CLOSE_BY_SHUTDOWN,

  /** An event representing a situation where a connection has been closed due to a timeout */
  CLOSE_BY_TIMEOUT;
}

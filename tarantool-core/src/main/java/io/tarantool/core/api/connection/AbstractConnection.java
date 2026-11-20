/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.api.connection;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.tarantool.core.api.connection.reason.CloseReason;
import io.tarantool.core.protocol.IProtoResponse;

public abstract class AbstractConnection implements Connection {

  private final Duration connectTimeout;
  private final Duration closeTimeout;
  private final Duration idleTimeout;
  private final InetSocketAddress address;

  /**
   * Callback invoked when the connection is closed for any reason, as reflected by a {@link
   * CloseReason}.
   *
   * @implSpec Subclasses must guarantee that the connection is already in a closed state when
   *     invoking this callback, and that it is invoked at most once.
   */
  protected final BiConsumer<Connection, CloseReason> onCloseHandler;

  /**
   * Callback for handling incoming responses from a Tarantool node, represented as {@link
   * IProtoResponse} objects.
   */
  protected final Consumer<IProtoResponse> responseHandler;

  protected AbstractConnection(
      Duration connectTimeout,
      Duration closeTimeout,
      Duration idleTimeout,
      InetSocketAddress address,
      BiConsumer<Connection, CloseReason> onCloseHandler,
      Consumer<IProtoResponse> responseHandler) {
    Objects.requireNonNull(connectTimeout, "connectTimeout must not be null!");
    Objects.requireNonNull(closeTimeout, "closeTimeout must not be null!");
    Objects.requireNonNull(idleTimeout, "idleTimeout must not be null!");

    this.connectTimeout = requiredPositive(connectTimeout, "connectTimeout must be > 0!");
    this.closeTimeout = requiredPositive(closeTimeout, "closeTimeout must be > 0!");
    this.address = Objects.requireNonNull(address, "address must not be null!");
    this.onCloseHandler =
        Objects.requireNonNull(onCloseHandler, "onCloseHandler must not be null!");
    this.responseHandler =
        Objects.requireNonNull(responseHandler, "responseHandler must not be null!");
    this.idleTimeout = idleTimeout;
  }

  @Override
  public InetSocketAddress address() {
    return this.address;
  }

  @Override
  public Duration connectTimeout() {
    return this.connectTimeout;
  }

  @Override
  public Duration closeTimeout() {
    return this.closeTimeout;
  }

  @Override
  public BiConsumer<Connection, CloseReason> onCloseHandler() {
    return this.onCloseHandler;
  }

  @Override
  public Consumer<IProtoResponse> responseHandler() {
    return this.responseHandler;
  }

  private static Duration requiredPositive(Duration timeout, String message) {
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException(message);
    }
    return timeout;
  }
}

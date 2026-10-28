/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool;

import java.util.function.Consumer;

import io.tarantool.core.IProtoClient;
import io.tarantool.pool.availability.Metadata;

/**
 * A functional-limited view of a {@link PoolEntry} object for modifying its visibility state in the
 * load balancer.
 *
 * <p>All methods provided by this interface are <b>guaranteed by the library to be non-blocking</b>
 * and are safe to invoke directly from any thread, including internal IO threads.
 */
public interface PoolEntryView {

  /**
   * Atomically sets the current availability state of the Tarantool node.
   *
   * @param state never null.
   * @implSpec This operation is non-blocking and idempotent. If the passed state is identical to
   *     the current one, the call has no effect and no state change events are fired.
   */
  void changeState(PoolEntryState state);

  /** Gets metadata about connection. */
  Metadata metadata();

  /**
   * Returns the current connection state.
   *
   * @implSpec must be not null.
   */
  PoolEntryState state();

  /**
   * Gets a client to make requests to determine availability status.
   *
   * @implNote Use this client to send push-style health checks that help determine whether to lock
   *     this connection from being handed off to the load balancer for request processing.
   * @implSpec Never close the client directly from the policy context. The returned client is
   *     guaranteed to be non-null, even if the underlying connection is temporarily down.
   */
  IProtoClient client();

  /**
   * Asynchronously schedules a full reconnection of both the network layer and the availability
   * policy.
   *
   * @implNote This method triggers a complete lifecycle recycle in the library. It sequentially
   *     executes: {@code policy stop} &rarr; {@code connection stop} &rarr; {@code connection
   *     start} &rarr; {@code policy start}. As a result, the current policy object will receive
   *     explicit {@code stop} and {@code start} invocations for this entry.
   * @implSpec This method is non-blocking and returns immediately.
   */
  void reconnect();

  /**
   * Asynchronously restarts only the availability policy (strategy) execution for this connection.
   *
   * @implNote This method forces the library to recycle the lifecycle of the current policy for
   *     this specific entry (effectively invoking {@code stop} followed by {@code start} on the
   *     policy). In contrast to {@link #reconnect()}, this method <b>does not</b> drop, stop, or
   *     restart the underlying physical network connection.
   * @implSpec This method is non-blocking and returns immediately.
   */
  void restart();

  /**
   * Emits events occurring in the connection and {@link
   * io.tarantool.pool.availability.AvailabilityPolicy}.
   *
   * @param action never null
   */
  void emit(Consumer<PoolEventListener> action);
}

/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.Timer;

import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEntryState;
import io.tarantool.pool.PoolEntryView;

/**
 * Policy interface to check the availability of a Tarantool node or a group of nodes.
 *
 * <p>The lifecycle of this policy is strictly managed by the library in the following order: {@code
 * configure} &rarr; {@code init} &rarr; {@code start} &rarr; {@code stop}.
 *
 * @apiNote This interface extends actions of {@link HeartbeatOpts} and allows defining a custom
 *     availability checking strategy. Notably, this interface can be specified for a single {@link
 *     InstanceConnectionGroup} or implement a strategy that handles multiple groups or connections.
 */
public interface AvailabilityPolicy {

  /**
   * Sets the {@link Timer} and {@link MeterRegistry} objects.
   *
   * @param timer never null
   * @apiNote Use this method to store objects useful in policy context to work with metrics and
   *     timers within the current policy object.
   */
  void configure(Timer timer, MeterRegistry metrics);

  /**
   * Sets the state value when the connection object is created. This method is called once upon
   * connection object creation.
   *
   * @apiNote Execution of the method for the current view is independent of the execution of this
   *     method for another view.
   * @param view never null.
   */
  void initStateBeforeAll(PoolEntryView view);

  /**
   * Returns the backoff delay duration before restarting the strategy execution for the given
   * connection.
   *
   * @param view the connection view. Never null.
   * @return the {@link Duration} to wait before the next {@code stop -> start} recycle sequence.
   *     Return {@link Duration#ZERO} for an immediate restart. Never null.
   * @implSpec This method can be invoked by the library at any time from any thread. The library
   *     guarantees that this method will be invoked <b>strictly after</b> {@link
   *     #initStateBeforeAll(PoolEntryView)} has been executed for the provided connection view. The
   *     implementation <b>must be strictly non-blocking, thread-safe, and return the value
   *     immediately (O(1))</b> without performing heavy computations or network calls.
   * @apiNote Execution of the method for the current view is independent of the execution of this
   *     method for another view.
   */
  Duration restartBackoff(PoolEntryView view);

  /**
   * Asynchronously starts the availability check task for the provided {@link PoolEntryView}.
   *
   * @param view the connection view. Never null.
   * @return a {@link CompletionStage} object that completes successfully if the availability check
   *     task for the provided {@link PoolEntryView} has started. Never null.
   * @implNote If the returned {@link CompletionStage} completes exceptionally, the library
   *     guarantees to log the exception and automatically trigger a policy lifecycle recycle by
   *     sequentially executing {@code stop} followed by {@code start} for this connection view.
   * @implSpec This method executes on an IO thread. The implementation <b>must return the {@link
   *     CompletionStage} immediately without blocking</b>. The actual initiation and execution of
   *     the background task must happen asynchronously and complete the stage later. Must be
   *     idempotent. All exceptions (including RuntimeExceptions) thrown synchronously must be
   *     caught and handled internally within the policy or reflected safely via the returned stage.
   * @apiNote Note that this method will be called concurrently by multiple {@code PoolEntry}
   *     instances if the same policy object is selected for different connections. At least, this
   *     method will be invoked for each connection within a single {@link InstanceConnectionGroup}.
   *     Execution of the method for the current view is independent of the execution of this method
   *     for another view.
   */
  CompletionStage<Void> start(PoolEntryView view);

  /**
   * @see #stop(PoolEntryView, Throwable)
   */
  default CompletionStage<Void> stop(PoolEntryView view) {
    return stop(view, null);
  }

  /**
   * Asynchronously terminates the background availability check for the provided {@link
   * PoolEntryView}.
   *
   * @param view not null.
   * @param cause the reason why this policy is stopped for a passed {@link PoolEntryView}. May be
   *     null.
   * @return a {@link CompletionStage} object that completes successfully if the availability check
   *     task for the provided {@link PoolEntryView} has stopped. Never null.
   * @implNote Note that this method will be called concurrently by multiple {@code
   *     io.tarantool.pool.PoolEntry} instances if the same policy object is selected for different
   *     connections. At least, this method will be invoked for each connection within a single
   *     {@link InstanceConnectionGroup}. Execution of the method for the current view is
   *     independent of the execution of this method for another view. It is guaranteed that this
   *     method will not be called before {@link #start(PoolEntryView)} for the provided {@link
   *     PoolEntryView}.
   *     <p>When the connection pool is closed, the library guarantees to invoke this method for
   *     every remaining active connection entry, allowing the policy to clean up any shared
   *     resources (e.g., background threads or executors).
   * @implSpec This method executes on an IO thread. The implementation <b>must return the {@link
   *     CompletionStage} immediately without blocking</b>. The actual termination of the background
   *     task and resource cleanup must happen asynchronously and complete the stage later. Must be
   *     idempotent. <b>All exceptions must be caught and handled internally</b>.
   *     <p><b>Crucial Architecture Requirement:</b> Upon invocation of this method, the
   *     implementation <b>must immediately change the connection state</b> via {@link
   *     PoolEntryView#changeState(PoolEntryState)} to a state that represents
   *     <b>unavailability</b>. This ensures that the connection is blocked from being handed off to
   *     the load balancer as fast as possible, especially during pool closure or subsequent
   *     reconnection phases.
   */
  CompletionStage<Void> stop(PoolEntryView view, Throwable cause);
}

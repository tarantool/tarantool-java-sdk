/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.lifecycle.Startable;

/** Represents TQE cluster that can start, stop, save mount data, restart. */
public interface TQECluster extends Startable {

  /** Stop cluster nodes with saving mount data and start them after delay. */
  default void restart(long delayBefore, TimeUnit unitBefore, long delayAfter, TimeUnit unitAfter)
      throws InterruptedException {

    final Collection<TarantoolContainer<?>> queue = queue().values();
    final Collection<GrpcContainer<?>> grpc = grpc().values();

    queue.parallelStream().forEach(TarantoolContainer::stopWithSafeMount);
    grpc.parallelStream().forEach(GrpcContainer::stop);

    if (delayBefore > 0) {
      unitBefore.sleep(delayBefore);
    }

    queue.parallelStream().forEach(TarantoolContainer::start);
    grpc.parallelStream().forEach(GrpcContainer::start);

    if (delayAfter > 0) {
      unitAfter.sleep(delayAfter);
    }
  }

  /** Returns name of TQE cluster */
  String clusterName();

  /** Returns Tarantool cluster nodes */
  Map<String, TarantoolContainer<?>> queue();

  /** Returns grpc nodes */
  Map<String, GrpcContainer<?>> grpc();
}

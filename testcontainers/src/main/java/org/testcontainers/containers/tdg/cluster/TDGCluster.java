/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdg.cluster;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.testcontainers.containers.tdg.TDGContainer;
import org.testcontainers.lifecycle.Startable;

/**
 * Represents the interface for classes that implement the {@code Tarantool Data Grid} cluster. Helps to manipulate
 * {@link TDGContainer}'s.
 */
public interface TDGCluster extends Startable {

  /**
   * Stop cluster nodes with saving mount data and start them after delay.
   */
  default void restart(long delayBefore, TimeUnit unitBefore, long delayAfter, TimeUnit unitAfter) throws InterruptedException {
    nodes().values().parallelStream().forEach(TDGContainer::stopWithSafeMount);

    if (delayBefore > 0) {
      unitBefore.sleep(delayBefore);
    }

    nodes().values().parallelStream().forEach(TDGContainer::start);

    if (delayAfter > 0) {
      unitAfter.sleep(delayAfter);
    }
  }

  /**
   * Get cluster nodes.
   */
  Map<String, TDGContainer<?>> nodes();

  /**
   * Get cluster name.
   */
  String clusterName();
}

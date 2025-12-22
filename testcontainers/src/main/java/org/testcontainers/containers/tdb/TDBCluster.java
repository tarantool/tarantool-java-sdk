/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdb;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.etcd.jetcd.launcher.EtcdContainer;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.tcm.TCMContainer;
import org.testcontainers.lifecycle.Startable;

public interface TDBCluster extends Startable {

  /**
   * Stop cluster nodes with saving mount data and start them after delay.
   */
  default void restart(long delay, TimeUnit unit) throws InterruptedException {
    nodes().values().parallelStream().forEach(TarantoolContainer::stopWithSafeMount);

    if (delay > 0) {
      unit.sleep(delay);
    }

    nodes().values().parallelStream().forEach(TarantoolContainer::start);
    TimeUnit.SECONDS.sleep(delay);
  }

  /**
   * Get cluster nodes.
   */
  Map<String, TarantoolContainer<?>> nodes();

  /**
   * Get cluster name.
   */
  String clusterName();

  /**
   * Get router nodes.
   */
  Map<String, TarantoolContainer<?>> routers();

  /**
   * Get all replica nodes (masters and slaves)
   */
  Map<String, TarantoolContainer<?>> storages();

  /**
   * Get etcd node. It's null if cluster is cluster of Tarantool 2.x.
   */
  EtcdContainer etcdContainer();

  /**
   * Get tcm node. It's null if cluster is cluster of Tarantool 2.x.
   */
  TCMContainer tcmContainer();
}

/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.testcontainers.containers.Container;
import org.testcontainers.lifecycle.Startable;

public interface TarantoolContainer<SELF extends TarantoolContainer<SELF>> extends Container<SELF>, Startable {

  Path DEFAULT_DATA_DIR = Paths.get("/", "data");

  int DEFAULT_TARANTOOL_PORT = 3301;

  default void restart(long delay, TimeUnit unit) throws InterruptedException {
    stopWithSafeMount();

    if (delay > 0) {
      unit.sleep(delay);
    }

    start();
  }

  /**
   * Specify path to Tarantool config file.
   */
  TarantoolContainer<SELF> withConfigPath(Path configPath);

  /**
   * Specify path to Tarantool migrations directory.
   */
  TarantoolContainer<SELF> withMigrationsPath(Path migrationsPath);

  /**
   * Get node name.
   */
  String node();

  /**
   * Get external address of node.
   */
  InetSocketAddress mappedAddress();

  /**
   * Get internal address of node.
   */
  InetSocketAddress internalAddress();

  /**
   * Stop container without deleting data directory
   */
  void stopWithSafeMount();
}

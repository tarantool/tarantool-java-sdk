/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdg;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.testcontainers.containers.Container;
import org.testcontainers.lifecycle.Startable;

/**
 * Common interface to represent {@code Tarantool Data Grid} containers with ability to save mount data. Implementations
 * must ensure that exposed ports are reserved after the first container launch.
 *
 * @param <SELF> type of {@link TDGContainer} implementation
 */
public interface TDGContainer<SELF extends TDGContainer<SELF>> extends Container<SELF>, Startable {

  /**
   * Default http port which is listening in {@code TDG}
   */
  int DEFAULT_HTTP_PORT = 8080;

  /**
   * Default location where data of TDG is stored .
   */
  Path DEFAULT_TDG_DATA_DIR = Paths.get("/", "var", "lib", "tarantool");

  /**
   * Stops container without deleting data directory. After calling this method, the container instance can be
   * restarted. <b><i>Note:</i></b> method must be idempotent.
   */
  void stopWithSafeMount();

  /**
   * Returns TDG node name. Returning name is alias of TDG container in docker network.
   *
   * @return node name
   */
  String node();

  /**
   * Returns http endpoint to give ability to connect to TDG container from host.
   *
   * @return http endpoint of TDG container
   */
  InetSocketAddress httpMappedAddress();

  /**
   * Returns iproto endpoint to give ability to connect to TDG container from host.
   *
   * @return iproto endpoint of TDG container
   */
  InetSocketAddress iprotoMappedAddress();

  /**
   * Stops container without save mount data. After calling this method, the container instance can't be restarted
   * (method {@code Container::start()} must throw exception). <b><i>Note:</i></b> method must be idempotent.
   */
  @Override
  void stop();
}

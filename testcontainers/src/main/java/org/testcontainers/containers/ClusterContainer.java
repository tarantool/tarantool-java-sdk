/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers;

/**
 * Common interface for Tarantool cluster containers (vshard-based and cartridge-based). Provides
 * access to the router endpoint and command execution.
 */
public interface ClusterContainer {

  /**
   * Get the router host.
   *
   * @return router hostname
   */
  String getHost();

  /**
   * Get the mapped router port (default router port 3301).
   *
   * @return mapped router port
   */
  int getPort();

  /**
   * Get the mapped port for the given original container port.
   *
   * @param originalPort the container-internal port
   * @return the mapped host port
   */
  Integer getMappedPort(int originalPort);

  /**
   * Execute a Lua command inside the cluster router.
   *
   * @param command a valid Lua command or sequence of Lua commands
   * @return command execution result
   */
  Container.ExecResult executeCommand(String command);
}

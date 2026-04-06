/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.cluster;

import java.util.Map;

import org.testcontainers.containers.Container;

/**
 * Common interface for Tarantool cluster containers (vshard-based and cartridge-based). Provides
 * access to the router endpoint and command execution.
 */
public interface ClusterContainer<T extends Container<T>> extends Container<T> {

  /** Returns cluster configurator used for post-startup cluster setup. */
  ClusterConfigurator<? extends ClusterContainer<?>> getConfigurator();

  /** Returns configured cluster name. */
  default String clusterName() {
    return getConfigurator().clusterName();
  }

  /** Returns all nodes handled by the configurator. */
  default Map<String, ? extends ClusterContainer<?>> nodes() {
    return getConfigurator().nodes();
  }

  /** Returns true if cluster topology was configured. */
  default boolean configured() {
    return getConfigurator().configured();
  }

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
   * Get the router username used for authentication.
   *
   * @return router username
   */
  String getUsername();

  /**
   * Get the router password used for authentication.
   *
   * @return router password
   */
  String getPassword();

  /**
   * Get the host directory bound into the container instance directory.
   *
   * @return normalized host directory path or empty string when binding is disabled
   */
  String getDirectoryBinding();

  /**
   * Get the container directory where cluster files are mounted.
   *
   * @return container instance directory
   */
  String getInstanceDir();

  /**
   * Get the internal router port exposed by the container.
   *
   * @return internal router port
   */
  int getInternalPort();

  /**
   * Execute a Lua script from classpath and decode the result.
   *
   * @param scriptResourcePath classpath path to the script
   * @param <V> decoded result type
   * @return decoded script execution result
   */
  <V> V executeScriptDecoded(String scriptResourcePath);

  /**
   * Execute a Lua script from classpath.
   *
   * @param scriptResourcePath classpath path to the script
   * @return script execution result
   */
  Container.ExecResult executeScript(String scriptResourcePath);

  /**
   * Execute a Lua command and decode the result.
   *
   * @param command a valid Lua command or sequence of Lua commands
   * @param <V> decoded result type
   * @return decoded command execution result
   */
  <V> V executeCommandDecoded(String command);

  /**
   * Execute a Lua command inside the cluster router.
   *
   * @param command a valid Lua command or sequence of Lua commands
   * @return command execution result
   */
  Container.ExecResult executeCommand(String command);
}

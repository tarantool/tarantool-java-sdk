/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers;

/** Represents operations available on a Tarantool Container */
public interface TarantoolContainerOperations<T extends Container<T>> extends Container<T> {
  /**
   * Get the Tarantool server exposed port for connecting the client to
   *
   * @return a port
   */
  int getPort();

  /**
   * Get the Tarantool user name for connecting the client with
   *
   * @return a user name
   */
  String getUsername();

  /**
   * Get the Tarantool user password for connecting the client with
   *
   * @return a user password
   */
  String getPassword();

  /**
   * Get the app scripts directory
   *
   * @return the app directory
   */
  String getDirectoryBinding();

  /**
   * Get the app scripts directory in the container
   *
   * @return the app scripts directory
   */
  String getInstanceDir();

  /**
   * Get the Tarantool server internal port for client connections
   *
   * @return a port
   */
  int getInternalPort();

  /**
   * Execute a local script in the Tarantool instance. The path must be classpath-relative.
   * `dofile()` function is executed internally, so possible exceptions will be caught as the client
   * exceptions.
   *
   * @param scriptResourcePath the classpath resource path to a script
   * @return script execution result
   * @throws Exception if failed to connect to the instance or execution fails
   */
  Container.ExecResult executeScript(String scriptResourcePath) throws Exception;

  /**
   * Execute a local script in the Tarantool instance. The path must be classpath-relative.
   * `dofile()` function is executed internally, so possible exceptions will be caught as the client
   * exceptions.
   *
   * @param <V> the result of script
   * @param scriptResourcePath the classpath resource path to a script
   * @return script execution result in {@link Container.ExecResult}
   * @throws Exception if failed to connect to the instance or execution fails
   */
  <V> V executeScriptDecoded(String scriptResourcePath) throws Exception;

  /**
   * Execute a command in the Tarantool instance. Example of a command: `return 1 + 2, 'foo'`
   *
   * @param command a valid Lua command or a sequence of Lua commands
   * @return command execution result
   * @throws Exception if failed to connect to the instance or execution fails
   */
  Container.ExecResult executeCommand(String command) throws Exception;

  /**
   * Execute a command in the Tarantool instance. Example of a command: `return 1 + 2, 'foo'`
   *
   * @param <V> the result of script
   * @param command a valid Lua command or a sequence of Lua commands
   * @return command execution result in {@link Container.ExecResult}
   * @throws Exception if failed to connect to the instance or execution fails
   */
  <V> V executeCommandDecoded(String command) throws Exception;
}

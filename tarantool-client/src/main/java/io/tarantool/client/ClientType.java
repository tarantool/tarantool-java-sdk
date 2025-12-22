/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client;

/**
 * Enumeration of client types that are used in the driver.
 */
public enum ClientType {
  /**
   * Represents a tarantool/crud client.
   */
  CRUD,

  /**
   * Represents a Tarantool Data Grid client.
   */
  TDG,

  /**
   * Represents a BOX client.
   */
  BOX;
  public static final ClientType DEFAULT_CLIENT_TYPE = ClientType.CRUD;
}

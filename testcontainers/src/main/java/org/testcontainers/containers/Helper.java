/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers;

public class Helper {

  public static boolean isCartridgeAvailable() {
    return System.getenv().getOrDefault("TARANTOOL_VERSION", "").matches("2.*");
  }
}

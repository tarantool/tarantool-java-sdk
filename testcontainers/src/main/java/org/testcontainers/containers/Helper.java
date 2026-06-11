/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers;

public class Helper {

  public static boolean isCartridgeAvailable() {
    return System.getenv().getOrDefault("TARANTOOL_VERSION", "").matches("2.*");
  }
}

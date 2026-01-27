/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.exceptions;

public class CartridgeTopologyException extends RuntimeException {
  static final String errorMsg = "Failed to change the app topology";

  public CartridgeTopologyException(String message) {
    super(message);
  }

  public CartridgeTopologyException(Throwable cause) {
    super(errorMsg, cause);
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection.exceptions;

/**
 * This is a basic class for exceptions related to low-level connection.
 *
 * @author Ivan Bannikov
 */
public class ConnectionException extends RuntimeException {

  private static final long serialVersionUID = 8142444224870694208L;

  public ConnectionException(String message) {
    super(message);
  }

  public ConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.exceptions;

/**
 * Basic exception class for server errors like outdated protocol version.
 *
 * @author Artyom Dubinin
 */
public class ServerException extends RuntimeException {

  private static final long serialVersionUID = -5142459997236825315L;

  public ServerException(String message) {
    super(message);
  }

  public ServerException(String message, Throwable cause) {
    super(message, cause);
  }

  public ServerException(String format, Object... args) {
    super(String.format(format, args));
  }
}

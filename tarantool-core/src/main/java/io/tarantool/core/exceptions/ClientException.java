/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.exceptions;

/**
 * Basic exception class for client errors like connection errors, configuration error etc
 *
 * @author Ivan Bannikov
 */
public class ClientException extends RuntimeException {

  private static final long serialVersionUID = 3900897937870734817L;

  public ClientException(String message) {
    super(message);
  }

  public ClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public ClientException(String format, Object... args) {
    super(String.format(format, args));
  }
}

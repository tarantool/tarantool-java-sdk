/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping;

/**
 * Basic exception class for client errors like connection errors, configuration error etc
 *
 * @author Artyom Dubinin
 */
public class JacksonMappingException extends RuntimeException {

  private static final long serialVersionUID = -4905584855557509881L;

  public JacksonMappingException(Throwable cause) {
    super(cause);
  }

  public JacksonMappingException(String message) {
    super(message);
  }
}

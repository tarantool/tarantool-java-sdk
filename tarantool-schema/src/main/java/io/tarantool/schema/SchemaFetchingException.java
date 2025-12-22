/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.schema;

import io.tarantool.core.exceptions.ClientException;

/**
 * @author Artyom Dubinin
 */
public class SchemaFetchingException extends ClientException {

  private static final long serialVersionUID = -2547674499006604813L;

  public SchemaFetchingException(String message) {
    super(message);
  }
}

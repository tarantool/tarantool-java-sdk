/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.schema;

import io.tarantool.core.exceptions.ClientException;

/**
 * @author Artyom Dubinin
 */
public class NoSchemaException extends ClientException {

  private static final long serialVersionUID = -5566781415655903514L;

  public NoSchemaException(String message) {
    super(message);
  }
}

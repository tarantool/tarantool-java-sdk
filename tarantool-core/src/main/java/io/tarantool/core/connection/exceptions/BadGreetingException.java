/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.connection.exceptions;

public class BadGreetingException extends ConnectionException {

  private static final long serialVersionUID = 2519225642360141488L;

  public BadGreetingException(String message) {
    super(message);
  }
}

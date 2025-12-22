/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.balancer.exceptions;

/**
 * Base class for Tarantool Pool runtime exceptions
 *
 * @author Ivan Bannikov
 */
public abstract class BalancerException extends RuntimeException {

  private static final long serialVersionUID = -1006520182718581935L;

  public BalancerException(String message) {
    super(message);
  }

  public BalancerException() {
    super();
  }

  public BalancerException(Throwable cause) {
    super(cause);
  }

  public BalancerException(String message, Throwable cause) {
    super(message, cause);
  }
}

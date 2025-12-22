/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.exceptions;

/**
 * Base class for Tarantool Pool runtime exceptions
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
public abstract class PoolException extends RuntimeException {

  private static final long serialVersionUID = 7272830166401038158L;

  public PoolException(String message) {
    super(message);
  }
}

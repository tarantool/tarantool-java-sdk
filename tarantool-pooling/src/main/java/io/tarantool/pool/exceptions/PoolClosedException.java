/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.exceptions;

/**
 * Exception thrown when an outer client tries request some connection from closed pool.
 *
 * @author<a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
public class PoolClosedException extends PoolException {

  private static final long serialVersionUID = -5293853988288785415L;

  public PoolClosedException(String message) {
    super(message);
  }
}

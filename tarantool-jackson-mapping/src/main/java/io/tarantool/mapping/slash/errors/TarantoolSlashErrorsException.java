/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.slash.errors;

/**
 * The base class of exceptions when working with the tarantool/errors library.
 *
 * @author <a href="https://github.com/artdu">Artyom Dubinin</a>
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public class TarantoolSlashErrorsException extends RuntimeException {

  /**
   * The reason for the exception.
   *
   * @see TarantoolSlashErrors
   */
  private final TarantoolSlashErrors reason;

  /**
   * Creates a {@link TarantoolSlashErrorsException} object with the given parameters.
   *
   * @param reason {@link TarantoolSlashErrors} object
   */
  public TarantoolSlashErrorsException(TarantoolSlashErrors reason) {
    super(reason.getStr());
    this.reason = reason;
  }

  /**
   * Returns value of reason field.
   *
   * @return {@link #reason} value.
   */
  public TarantoolSlashErrors getReason() {
    return reason;
  }
}

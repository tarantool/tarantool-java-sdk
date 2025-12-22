/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping.slash.errors;

/**
 * <p>The base class of exceptions when working with the tarantool/errors library.</p>
 *
 * @author <a href="https://github.com/artdu">Artyom Dubinin</a>
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public class TarantoolSlashErrorsException extends RuntimeException {

  /**
   * <p>The reason for the exception.</p>
   *
   * @see TarantoolSlashErrors
   */
  private final TarantoolSlashErrors reason;

  /**
   * <p>Creates a {@link TarantoolSlashErrorsException} object with the given parameters.</p>
   *
   * @param reason {@link TarantoolSlashErrors} object
   */
  public TarantoolSlashErrorsException(TarantoolSlashErrors reason) {
    super(reason.getStr());
    this.reason = reason;
  }

  /**
   * <p>Returns value of reason field.</p>
   *
   * @return {@link #reason} value.
   */
  public TarantoolSlashErrors getReason() {
    return reason;
  }
}

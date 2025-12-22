/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping.crud;

import io.tarantool.mapping.slash.errors.TarantoolSlashErrorsException;

/**
 * <p>The base class of exceptions when working with the crud.</p>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public class CrudException extends TarantoolSlashErrorsException {

  /**
   * <p>Creates a {@link CrudException} object with the given parameters.</p>
   *
   * @param reason {@link CrudError} object
   */
  public CrudException(CrudError reason) {
    super(reason);
  }
}

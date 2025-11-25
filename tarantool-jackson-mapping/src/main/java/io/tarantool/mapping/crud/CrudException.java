/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.crud;

import io.tarantool.mapping.slash.errors.TarantoolSlashErrorsException;

/**
 * The base class of exceptions when working with the crud.
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public class CrudException extends TarantoolSlashErrorsException {

  /**
   * Creates a {@link CrudException} object with the given parameters.
   *
   * @param reason {@link CrudError} object
   */
  public CrudException(CrudError reason) {
    super(reason);
  }
}

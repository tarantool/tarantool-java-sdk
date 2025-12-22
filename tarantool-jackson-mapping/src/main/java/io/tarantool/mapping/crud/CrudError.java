/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping.crud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.tarantool.mapping.slash.errors.TarantoolSlashErrors;

/**
 * <p>The class implements error that comes in response to CRUD operations from Tarantool.</p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see <a href="https://github.com/tarantool/crud">crud</a>
 * @see <a href="https://github.com/tarantool/errors">errors</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrudError extends TarantoolSlashErrors {

  @Override
  public String toString() {
    return "CrudError{" +
        "err='" + err + '\'' +
        ", str='" + str + '\'' +
        ", line=" + line +
        ", file='" + file + '\'' +
        ", stack='" + stack + '\'' +
        ", className='" + className + '\'' +
        '}';
  }
}

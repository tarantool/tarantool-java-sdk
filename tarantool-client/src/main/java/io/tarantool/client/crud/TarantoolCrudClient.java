/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.crud;

import io.tarantool.client.ClientType;
import io.tarantool.client.TarantoolClient;

/**
 * <p>Implements a contract for a client working with spaces through
 * <a href="https://www.tarantool.io/en/doc/latest/dev_guide/internals/box_protocol/">CRUD</a> module.</p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public interface TarantoolCrudClient extends TarantoolClient {

  /**
   * <p>Default username for CRUD clients.</p>
   */
  String DEFAULT_CRUD_USERNAME = "admin";

  /**
   * <p>Default password for CRUD admin username.</p>
   */
  String DEFAULT_CRUD_PASSWORD = "secret-cluster-cookie";

  /**
   * <p>Function returns  {@link TarantoolCrudSpace space} with the name specified as the input argument.</p>
   *
   * @param name name of the {@link TarantoolCrudSpace space} that was requested.
   * @return {@link TarantoolCrudSpace} object.
   */
  TarantoolCrudSpace space(String name);

  /**
   * <p>Returns {@link ClientType} of this client.</p>
   *
   * @return {@link ClientType} object
   */
  default ClientType getType() {
    return ClientType.CRUD;
  }
}

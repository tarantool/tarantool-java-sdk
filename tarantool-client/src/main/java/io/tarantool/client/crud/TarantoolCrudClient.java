/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.crud;

import io.tarantool.client.ClientType;
import io.tarantool.client.TarantoolClient;

/**
 * Implements a contract for a client working with spaces through <a
 * href="https://www.tarantool.io/en/doc/latest/dev_guide/internals/box_protocol/">CRUD</a> module.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public interface TarantoolCrudClient extends TarantoolClient {

  /** Default username for CRUD clients. */
  String DEFAULT_CRUD_USERNAME = "api_user";

  /** Default password for CRUD admin username. */
  String DEFAULT_CRUD_PASSWORD = "secret";

  /**
   * Function returns {@link TarantoolCrudSpace space} with the name specified as the input
   * argument.
   *
   * @param name name of the {@link TarantoolCrudSpace space} that was requested.
   * @return {@link TarantoolCrudSpace} object.
   */
  TarantoolCrudSpace space(String name);

  /**
   * Returns {@link ClientType} of this client.
   *
   * @return {@link ClientType} object
   */
  default ClientType getType() {
    return ClientType.CRUD;
  }
}

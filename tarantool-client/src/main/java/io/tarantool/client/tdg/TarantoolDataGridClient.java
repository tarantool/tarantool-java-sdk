/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.tdg;

import io.tarantool.client.ClientType;
import io.tarantool.client.TarantoolClient;

/**
 * Implements a contract for a client working with spaces through <a
 * href="https://www.tarantool.io/en/tdg/latest/development/iproto/#tdg-iproto-repository">TDG
 * repository interface</a>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 */
public interface TarantoolDataGridClient extends TarantoolClient {

  /** Default username for TDG clients. */
  String DEFAULT_TDG_USERNAME = "tdg_service_user";

  /** Default password for TDG admin username. */
  String DEFAULT_TDG_PASSWORD = "";

  /**
   * Function returns {@link TarantoolDataGridSpace space} with the name specified as the input
   * argument.
   *
   * @param name name of the {@link TarantoolDataGridSpace space} that was requested.
   * @return {@link TarantoolDataGridSpace} object.
   */
  TarantoolDataGridSpace space(String name);

  /**
   * Returns {@link ClientType} of this client.
   *
   * @return {@link ClientType} object
   */
  default ClientType getType() {
    return ClientType.TDG;
  }
}

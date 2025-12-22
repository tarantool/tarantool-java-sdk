/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.tdg;

import io.tarantool.client.ClientType;
import io.tarantool.client.TarantoolClient;

/**
 * <p>Implements a contract for a client working with spaces through
 * <a href="https://www.tarantool.io/en/tdg/latest/development/iproto/#tdg-iproto-repository">
 *   TDG repository interface</a>
 * </p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 */
public interface TarantoolDataGridClient extends TarantoolClient {

  /**
   * <p>Default username for TDG clients.</p>
   */
  String DEFAULT_TDG_USERNAME = "tdg_service_user";

  /**
   * <p>Default password for TDG admin username.</p>
   */
  String DEFAULT_TDG_PASSWORD = "";

  /**
   * <p>Function returns  {@link TarantoolDataGridSpace space} with the name specified as the input argument.</p>
   *
   * @param name name of the {@link TarantoolDataGridSpace space} that was requested.
   * @return {@link TarantoolDataGridSpace} object.
   */
  TarantoolDataGridSpace space(String name);

  /**
   * <p>Returns {@link ClientType} of this client.</p>
   *
   * @return {@link ClientType} object
   */
  default ClientType getType() {
    return ClientType.TDG;
  }
}

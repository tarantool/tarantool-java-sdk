/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.box;

import java.util.concurrent.CompletableFuture;

import io.tarantool.client.ClientType;
import io.tarantool.client.TarantoolClient;
import io.tarantool.client.TarantoolVersion;
import io.tarantool.schema.TarantoolSchemaFetcher;

/**
 * Implements a contract for a client working with spaces through <a
 * href="https://www.tarantool.io/en/doc/latest/dev_guide/internals/box_protocol/">binary
 * protocol</a>.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public interface TarantoolBoxClient extends TarantoolClient {

  /** Default username for BOX client. */
  String DEFAULT_BOX_USERNAME = "guest";

  /** Default fetch schema policy. */
  boolean DEFAULT_FETCH_SCHEMA = true;

  /** Default ignore old schema version policy. */
  boolean DEFAULT_IGNORE_OLD_SCHEMA_VERSION = true;

  /**
   * Function returns {@link TarantoolBoxSpace space} with the identifier specified as the input
   * argument.
   *
   * @param id id of the {@link TarantoolBoxSpace space} that was requested.
   * @return {@link TarantoolBoxSpace} object.
   */
  TarantoolBoxSpace space(int id);

  /**
   * Function returns {@link TarantoolBoxSpace space} with the name specified as the input argument.
   *
   * @param name name of the {@link TarantoolBoxSpace space} that was requested.
   * @return {@link TarantoolBoxSpace} object.
   */
  TarantoolBoxSpace space(String name);

  /**
   * Special class that contains information about spaces.
   *
   * @return {@link TarantoolSchemaFetcher} object.
   */
  TarantoolSchemaFetcher getFetcher();

  /**
   * Returns {@link ClientType} of this client.
   *
   * @return {@link ClientType} object
   */
  default ClientType getType() {
    return ClientType.BOX;
  }

  /**
   * Returns {@link CompletableFuture} with TarantoolVersion.
   *
   * @return {@link CompletableFuture} object. If successful - future is completed with a
   *     TarantoolVersion structure, otherwise this future will be completed exceptionally.
   */
  CompletableFuture<TarantoolVersion> getServerVersion();
}

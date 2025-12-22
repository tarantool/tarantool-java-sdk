/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.box;

import java.util.concurrent.CompletableFuture;
import io.netty.util.concurrent.CompleteFuture;
import io.tarantool.client.ClientType;
import io.tarantool.client.TarantoolClient;
import io.tarantool.client.TarantoolVersion;
import io.tarantool.schema.TarantoolSchemaFetcher;

/**
 * <p>Implements a contract for a client working with spaces through
 * <a href="https://www.tarantool.io/en/doc/latest/dev_guide/internals/box_protocol/">binary protocol</a>.</p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public interface TarantoolBoxClient extends TarantoolClient {

  /**
   * <p>Default username for BOX client.</p>
   */
  String DEFAULT_BOX_USERNAME = "guest";

  /**
   * <p>Default fetch schema policy.</p>
   */
  boolean DEFAULT_FETCH_SCHEMA = true;

  /**
   * <p>Default ignore old schema version policy.</p>
   */
  boolean DEFAULT_IGNORE_OLD_SCHEMA_VERSION = true;


  /**
   * <p>Function returns {@link TarantoolBoxSpace space} with the identifier specified as the input argument.</p>
   *
   * @param id id of the {@link TarantoolBoxSpace space} that was requested.
   * @return {@link TarantoolBoxSpace} object.
   */
  TarantoolBoxSpace space(int id);

  /**
   * <p>Function returns  {@link TarantoolBoxSpace space} with the name specified as the input argument.</p>
   *
   * @param name name of the {@link TarantoolBoxSpace space} that was requested.
   * @return {@link TarantoolBoxSpace} object.
   */
  TarantoolBoxSpace space(String name);

  /**
   * <p>Special class that contains information about spaces.</p>
   *
   * @return {@link TarantoolSchemaFetcher} object.
   */
  TarantoolSchemaFetcher getFetcher();

  /**
   * <p>Returns {@link ClientType} of this client.</p>
   *
   * @return {@link ClientType} object
   */
  default ClientType getType() {
    return ClientType.BOX;
  }

  /**
   * <p>
   * Returns {@link CompletableFuture} with TarantoolVersion.
   * </p>
   *
   * @return {@link CompletableFuture} object. If successful - future is completed with a
   *         TarantoolVersion structure, otherwise this future will be completed exceptionally.
   */
  CompletableFuture<TarantoolVersion> getServerVersion();
}

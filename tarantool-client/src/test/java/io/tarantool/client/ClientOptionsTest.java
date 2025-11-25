/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import io.netty.channel.ChannelOption;
import org.junit.jupiter.api.Test;

import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolFactory;

/**
 * @author Artyom Dubinin
 */
public class ClientOptionsTest {

  @Test
  public void testCrudClientChannelOption() throws Exception {
    TarantoolCrudClient simpleCrudClient = TarantoolFactory.crud().build();
    assertEquals(
        new HashMap<ChannelOption, Object>() {
          {
            put(ChannelOption.TCP_NODELAY, true);
            put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000);
            put(ChannelOption.SO_KEEPALIVE, true);
            put(ChannelOption.SO_REUSEADDR, true);
          }
        },
        simpleCrudClient.getPool().getFactory().getBootstrap().config().options());
    TarantoolCrudClient crudClientWithOneRemovedChannelOption =
        assertDoesNotThrow(
            () ->
                TarantoolFactory.crud()
                    .withChannelOption(ChannelOption.SO_KEEPALIVE, null)
                    .build());
    assertEquals(
        new HashMap<ChannelOption, Object>() {
          {
            put(ChannelOption.TCP_NODELAY, true);
            put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000);
            put(ChannelOption.SO_REUSEADDR, true);
          }
        },
        crudClientWithOneRemovedChannelOption
            .getPool()
            .getFactory()
            .getBootstrap()
            .config()
            .options());
  }

  @Test
  public void testCrudClientChannelOptions() {
    TarantoolCrudClient crudClient =
        assertDoesNotThrow(
            () ->
                TarantoolFactory.crud()
                    .withChannelOptions(
                        Collections.unmodifiableMap(
                            new HashMap<ChannelOption<?>, Object>() {
                              {
                                put(ChannelOption.CONNECT_TIMEOUT_MILLIS, null);
                                put(ChannelOption.TCP_NODELAY, null);
                                put(ChannelOption.SO_KEEPALIVE, null);
                                put(ChannelOption.SO_REUSEADDR, null);
                              }
                            }))
                    .build());
    assertEquals(
        Collections.emptyMap(),
        crudClient.getPool().getFactory().getBootstrap().config().options());
  }

  @Test
  public void testBoxClientChannelOption() throws Exception {
    TarantoolBoxClient simpleBoxClient = TarantoolFactory.box().withFetchSchema(false).build();
    assertEquals(
        new HashMap<ChannelOption, Object>() {
          {
            put(ChannelOption.TCP_NODELAY, true);
            put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000);
            put(ChannelOption.SO_KEEPALIVE, true);
            put(ChannelOption.SO_REUSEADDR, true);
          }
        },
        simpleBoxClient.getPool().getFactory().getBootstrap().config().options());
    TarantoolBoxClient boxClientWithOneRemovedChannelOption =
        assertDoesNotThrow(
            () ->
                TarantoolFactory.box()
                    .withChannelOption(ChannelOption.SO_KEEPALIVE, null)
                    .withFetchSchema(false)
                    .build());
    assertEquals(
        new HashMap<ChannelOption, Object>() {
          {
            put(ChannelOption.TCP_NODELAY, true);
            put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000);
            put(ChannelOption.SO_REUSEADDR, true);
          }
        },
        boxClientWithOneRemovedChannelOption
            .getPool()
            .getFactory()
            .getBootstrap()
            .config()
            .options());
  }

  @Test
  public void testBoxClientChannelOptions() {
    TarantoolBoxClient boxClient =
        assertDoesNotThrow(
            () ->
                TarantoolFactory.box()
                    .withChannelOptions(
                        Collections.unmodifiableMap(
                            new HashMap<ChannelOption<?>, Object>() {
                              {
                                put(ChannelOption.CONNECT_TIMEOUT_MILLIS, null);
                                put(ChannelOption.TCP_NODELAY, null);
                                put(ChannelOption.SO_KEEPALIVE, null);
                                put(ChannelOption.SO_REUSEADDR, null);
                              }
                            }))
                    .withFetchSchema(false)
                    .build());
    assertEquals(
        Collections.emptyMap(), boxClient.getPool().getFactory().getBootstrap().config().options());
  }
}

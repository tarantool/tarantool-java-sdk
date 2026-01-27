/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client;

// --8<-- [start:all]

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import testcontainers.utils.TarantoolSingleNodeConfigUtils;

import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.factory.TarantoolBoxClientBuilder;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.pool.InstanceConnectionGroup;

public class TarantoolSingleInstanceConnectionTJSDKExample
    extends TarantoolSingleInstanceConnectionAbstractExample {

  @Test
  protected void simpleConnection() {
    // Получаем адрес и порт из докера
    // Gets address and port from docker
    final InetSocketAddress nodeAddress = CONTAINER.mappedAddress();

    // Настраиваем группу подключения
    // Set ups connection group
    final InstanceConnectionGroup connectionGroup =
        InstanceConnectionGroup.builder()
            .withHost(nodeAddress.getHostName())
            .withPort(nodeAddress.getPort())
            .withUser(TarantoolSingleNodeConfigUtils.LOGIN)
            .withPassword(TarantoolSingleNodeConfigUtils.PWD.toString())
            .build();

    final TarantoolBoxClientBuilder clientBuilder =
        TarantoolFactory.box().withGroups(Collections.singletonList(connectionGroup));

    try (final TarantoolBoxClient singleNodeClient = clientBuilder.build()) {

      final TarantoolResponse<List<String>> response =
          singleNodeClient.eval("return _TARANTOOL", String.class).join();
      final List<String> results = response.get();

      Assertions.assertEquals(1, results.size());
      Assertions.assertTrue(results.get(0).contains(TARANTOOL_TAG));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
// --8<-- [end:all]

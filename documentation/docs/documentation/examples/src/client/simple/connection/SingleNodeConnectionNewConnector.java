/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client.simple.connection;

// --8<-- [start:new-simple-connection]

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import testcontainers.single.CreateSingleNode;

import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.factory.TarantoolBoxClientBuilder;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.pool.InstanceConnectionGroup;

public class SingleNodeConnectionNewConnector extends SingleNodeConnection {

  @Test
  @Override
  protected void simpleConnection() {
    // Получаем адрес и порт из докера
    final InetSocketAddress nodeAddress = CONTAINER.mappedAddress();

    // Настраиваем группу подключения
    final InstanceConnectionGroup connectionGroup =
        InstanceConnectionGroup.builder()
            .withHost(nodeAddress.getHostName())
            .withPort(nodeAddress.getPort())
            .withUser(CreateSingleNode.LOGIN)
            .withPassword(CreateSingleNode.PWD.toString())
            .build();

    final TarantoolBoxClientBuilder clientBuilder =
        TarantoolFactory.box().withGroups(Collections.singletonList(connectionGroup));

    try (final TarantoolBoxClient singleNodeClient = clientBuilder.build()) {

      final TarantoolResponse<List<String>> response =
          singleNodeClient.eval("return _TARANTOOL", String.class).join();
      final List<String> results = response.get();

      Assertions.assertEquals(1, results.size());
      Assertions.assertTrue(results.get(0).contains("3.4.1"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
// --8<-- [end:new-simple-connection]

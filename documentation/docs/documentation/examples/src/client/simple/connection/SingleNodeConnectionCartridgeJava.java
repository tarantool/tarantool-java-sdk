/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client.simple.connection;

// --8<-- [start:old-simple-connection]

import java.net.InetSocketAddress;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import testcontainers.single.CreateSingleNode;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;

public class SingleNodeConnectionCartridgeJava extends SingleNodeConnection {

  @Test
  @Override
  protected void simpleConnection() {
    try (TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client = setupClient()) {
      final List<?> result = client.eval("return _TARANTOOL").join();

      Assertions.assertEquals(1, result.size());

      final Object object = result.get(0);

      Assertions.assertInstanceOf(String.class, object);
      Assertions.assertTrue(((String) object).contains("3.4.1"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> setupClient() {
    // Получаем адрес и порт из докера
    final InetSocketAddress nodeAddress = CONTAINER.mappedAddress();

    return TarantoolClientFactory.createClient()
        .withAddress(nodeAddress.getHostName(), nodeAddress.getPort())
        .withCredentials(CreateSingleNode.LOGIN, CreateSingleNode.PWD.toString())
        .build();
  }
}
// --8<-- [end:old-simple-connection]

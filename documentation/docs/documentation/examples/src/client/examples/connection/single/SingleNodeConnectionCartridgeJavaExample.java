/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client.examples.connection.single;

// --8<-- [start:old-simple-connection]

import java.net.InetSocketAddress;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import testcontainers.utils.SingleNodeConfigUtils;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;

public class SingleNodeConnectionCartridgeJavaExample extends SingleNodeConnectionAbstractExample {

  @Test
  @Override
  protected void simpleConnection() {
    try (TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client = setupClient()) {
      final List<?> result = client.eval("return _TARANTOOL").join();

      Assertions.assertEquals(1, result.size());

      final Object object = result.get(0);

      Assertions.assertInstanceOf(String.class, object);
      Assertions.assertTrue(((String) object).contains(TARANTOOL_TAG));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> setupClient() {
    // Получаем адрес и порт из докера
    // Gets address and port from docker
    final InetSocketAddress nodeAddress = CONTAINER.mappedAddress();

    return TarantoolClientFactory.createClient()
        .withAddress(nodeAddress.getHostName(), nodeAddress.getPort())
        .withCredentials(SingleNodeConfigUtils.LOGIN, SingleNodeConfigUtils.PWD.toString())
        .build();
  }
}
// --8<-- [end:old-simple-connection]

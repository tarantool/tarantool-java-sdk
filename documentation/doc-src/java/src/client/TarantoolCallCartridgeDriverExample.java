/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client;

// --8<-- [start:all]

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import testcontainers.utils.TarantoolSingleNodeConfigUtils;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;

public class TarantoolCallCartridgeDriverExample extends TarantoolCallEvalAbstractExample {

  @Test
  @SuppressWarnings("unchecked")
  void simpleCall() {
    try (TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client = setupClient()) {
      loadFunctions();

      final List<?> helloWorldReturns = client.call(HELLO_WORLD_FUNCTION).join();
      Assertions.assertEquals(1, helloWorldReturns.size());
      Assertions.assertEquals(HELLO_WORLD_FUNCTION_RETURNS, helloWorldReturns.get(0));

      // convert returning lua map into Java pojo representing as map
      final String name = "Petya";
      final int age = 25;

      final List<?> resultAsMap = client.call(SOME_MAP_FUNCTION, Arrays.asList(name, age)).join();

      Assertions.assertEquals(1, resultAsMap.size());
      Assertions.assertInstanceOf(Map.class, resultAsMap.get(0));
      final Map<String, Object> castedResult = (Map<String, Object>) resultAsMap.get(0);
      Assertions.assertEquals(name, castedResult.get("name"));
      Assertions.assertEquals(age, castedResult.get("age"));

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
        .withCredentials(
            TarantoolSingleNodeConfigUtils.LOGIN, TarantoolSingleNodeConfigUtils.PWD.toString())
        .build();
  }
}

// --8<-- [end:all]

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client;

// --8<-- [start:all]

import java.net.InetSocketAddress;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.TarantoolServerAddress;
import io.tarantool.driver.api.tuple.TarantoolTuple;

public class TarantoolDBClusterConnectionCartridgeDriverExample
    extends TarantoolDBClusterConnectionAbstractExample {

  @Test
  @Override
  protected void simpleCrudConnection() {
    try (TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> crudClient =
        setupClient()) {
      final String helloWorld = "hello world";

      // Evals return instruction in Tarantool lua
      final List<?> helloResponse =
          crudClient.eval(String.format("return '%s'", helloWorld)).join();
      Assertions.assertEquals(1, helloResponse.size());
      Assertions.assertEquals(helloWorld, helloResponse.get(0));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> setupClient() {

    // Returns routers addresses mapped from docker
    final InetSocketAddress firstRouterAddress = getRouterAddress(FIRST_ROUTER_CONTAINER_NAME);
    final InetSocketAddress secondRouterAddress = getRouterAddress(SECOND_ROUTER_CONTAINER_NAME);

    // Create crud client instance and connect to routers
    return TarantoolClientFactory.createClient()
        .withAddresses(
            new TarantoolServerAddress(
                firstRouterAddress.getHostName(), firstRouterAddress.getPort()),
            new TarantoolServerAddress(
                secondRouterAddress.getHostName(), secondRouterAddress.getPort()))

        // Two connection groups with different users in one client instance
        .withCredentials(USER_1182, USER_1182_PWD)
        .withConnections(2)
        .withProxyMethodMapping()
        .build();
  }
}

// --8<-- [end:all]

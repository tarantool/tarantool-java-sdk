/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client;

// --8<-- [start:all]

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolCrudClientBuilder;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.pool.InstanceConnectionGroup;

public class TarantoolDBClusterConnectionTJSDKExample
    extends TarantoolDBClusterConnectionAbstractExample {

  @Test
  @Override
  protected void simpleCrudConnection() {

    try (final TarantoolCrudClient crudClient = setupClient()) {
      final String helloWorld = "hello world";

      // Evals return instruction in Tarantool lua
      final List<String> helloResponse =
          crudClient.eval(String.format("return '%s'", helloWorld), String.class).join().get();
      Assertions.assertEquals(1, helloResponse.size());
      Assertions.assertEquals(helloWorld, helloResponse.get(0));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static TarantoolCrudClient setupClient() throws Exception {

    // Returns routers addresses mapped from docker
    final InetSocketAddress firstRouterAddress = getRouterAddress(FIRST_ROUTER_CONTAINER_NAME);
    final InetSocketAddress secondRouterAddress = getRouterAddress(SECOND_ROUTER_CONTAINER_NAME);

    final TarantoolCrudClientBuilder crudClientBuilder = TarantoolFactory.crud();

    // Setup first connection group with "seller-user" user and 2 connection to first router
    final InstanceConnectionGroup firstRouterConnectionGroup =
        InstanceConnectionGroup.builder()
            .withHost(firstRouterAddress.getHostName())
            .withPort(firstRouterAddress.getPort())
            .withUser(SELLER_USER)
            .withPassword(SELLER_USER_PWD)
            .withSize(2)
            .withTag(SELLER_USER + "-connection")
            .build();

    // Setup second connection group with "user-1182" user and 3 connection to first router
    final InstanceConnectionGroup secondRouterConnectionGroup =
        InstanceConnectionGroup.builder()
            .withHost(secondRouterAddress.getHostName())
            .withPort(secondRouterAddress.getPort())
            .withUser(USER_1182)
            .withPassword(USER_1182_PWD)
            .withSize(3)
            .withTag(USER_1182 + "-connection")
            .build();

    final List<InstanceConnectionGroup> connectionGroupsList =
        Arrays.asList(firstRouterConnectionGroup, secondRouterConnectionGroup);

    // Create crud client instance and connect to routers
    // Two connection groups with different users in one client instance
    return crudClientBuilder.withGroups(connectionGroupsList).build();
  }
}

// --8<-- [end:all]

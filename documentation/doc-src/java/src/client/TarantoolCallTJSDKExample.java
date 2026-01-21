/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client;

// --8<-- [start:all]

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import testcontainers.utils.TarantoolSingleNodeConfigUtils;

import io.tarantool.client.TarantoolClient;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.pool.InstanceConnectionGroup;

public class TarantoolCallTJSDKExample extends TarantoolCallEvalAbstractExample {

  @Test
  void simpleCall() {
    try (TarantoolClient client = setupClient()) {
      loadFunctions();

      final List<String> helloWorldReturns =
          client.call(HELLO_WORLD_FUNCTION, String.class).join().get();
      Assertions.assertEquals(1, helloWorldReturns.size());
      Assertions.assertEquals(HELLO_WORLD_FUNCTION_RETURNS, helloWorldReturns.get(0));

      // convert returning lua map into Java pojo representing as map
      final String name = "Petya";
      final int age = 25;

      final List<TestUser> resultAsPojo =
          client.call(SOME_MAP_FUNCTION, Arrays.asList(name, age), TestUser.class).join().get();

      Assertions.assertEquals(1, resultAsPojo.size());
      Assertions.assertEquals(name, resultAsPojo.get(0).name());
      Assertions.assertEquals(age, resultAsPojo.get(0).age());

      // convert returning lua map into java list of map via type reference
      // java map key type is string because lua map key type is string!
      final List<Map<String, Object>> objectObjectMap =
          client
              .call(
                  SOME_MAP_FUNCTION,
                  Arrays.asList(name, age),
                  new TypeReference<List<Map<String, Object>>>() {})
              .join()
              .get();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static TarantoolClient setupClient() throws Exception {
    final InetSocketAddress address = CONTAINER.mappedAddress();

    final InstanceConnectionGroup connectionGroup =
        InstanceConnectionGroup.builder()
            .withHost(address.getHostName())
            .withPort(address.getPort())
            .withUser(TarantoolSingleNodeConfigUtils.LOGIN)
            .withPassword(TarantoolSingleNodeConfigUtils.PWD.toString())
            .withSize(3)
            .build();

    return TarantoolFactory.box().withGroups(Collections.singletonList(connectionGroup)).build();
  }
}

// --8<-- [end:all]

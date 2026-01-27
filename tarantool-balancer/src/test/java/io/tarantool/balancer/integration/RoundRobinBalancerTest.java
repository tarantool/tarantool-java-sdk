/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.balancer.integration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.msgpack.value.ValueFactory;
import org.testcontainers.containers.tarantool.TarantoolContainerImpl;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.balancer.TarantoolRoundRobinBalancer;
import io.tarantool.core.IProtoClient;
import io.tarantool.pool.IProtoClientPool;
import io.tarantool.pool.IProtoClientPoolImpl;
import io.tarantool.pool.InstanceConnectionGroup;

@Timeout(value = 5)
@Testcontainers
public class RoundRobinBalancerTest extends BaseTest {

  @Container
  private static final TarantoolContainerImpl tt1 = new TarantoolContainerImpl().withEnv(ENV_MAP);

  @Container
  private static final TarantoolContainerImpl tt2 = new TarantoolContainerImpl().withEnv(ENV_MAP);

  @BeforeAll
  public static void setUp() {
    count1 = ThreadLocalRandom.current().nextInt(MIN_CONNECTION_COUNT, MAX_CONNECTION_COUNT + 1);
    count2 = ThreadLocalRandom.current().nextInt(MIN_CONNECTION_COUNT, MAX_CONNECTION_COUNT + 1);
  }

  private int getSessionCounter(TarantoolContainerImpl tt) throws Exception {
    List<?> result = tt.executeCommandDecoded("return get_session_counter()");
    return (Integer) result.get(0);
  }

  private int getCallCounter(TarantoolContainerImpl tt) throws Exception {
    List<?> result = tt.executeCommandDecoded("return get_call_counter()");
    return (Integer) result.get(0);
  }

  @Test
  public void testRoundRobin() throws Exception {
    IProtoClientPool pool = new IProtoClientPoolImpl(factory, timerResource);
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(tt1.getHost())
                .withPort(tt1.getPort())
                .withSize(count1)
                .withTag("node-a")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost(tt2.getHost())
                .withPort(tt2.getPort())
                .withSize(count2)
                .withTag("node-b")
                .build()));

    TarantoolBalancer rrBalancer = new TarantoolRoundRobinBalancer(pool);
    for (int i = 0; i < (count1 + count2) * 2; i++) {
      IProtoClient client = rrBalancer.getNext().get();
      client.authorize(API_USER, CREDS.get(API_USER)).join();
      client.eval("inc()", ValueFactory.emptyArray()).get();
    }
    assertEquals(count1, getSessionCounter(tt1));
    assertEquals(count2, getSessionCounter(tt2));
    assertEquals(count1 * 2, getCallCounter(tt1));
    assertEquals(count2 * 2, getCallCounter(tt2));

    pool.close();
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.balancer.integration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.containers.utils.TarantoolContainerClientHelper.createTarantoolContainer;
import static org.testcontainers.containers.utils.TarantoolContainerClientHelper.execInitScript;
import static org.testcontainers.containers.utils.TarantoolContainerClientHelper.executeCommandDecoded;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.msgpack.value.ValueFactory;
import org.testcontainers.containers.tarantool.TarantoolContainer;

import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.balancer.TarantoolRoundRobinBalancer;
import io.tarantool.core.IProtoClient;
import io.tarantool.pool.IProtoClientPool;
import io.tarantool.pool.IProtoClientPoolImpl;
import io.tarantool.pool.InstanceConnectionGroup;

@Timeout(value = 5)
public class RoundRobinBalancerTest extends BaseTest {

  private static TarantoolContainer<?> tt1;
  private static TarantoolContainer<?> tt2;

  @BeforeAll
  public static void setUp() {
    tt1 = createTarantoolContainer();
    tt2 = createTarantoolContainer();

    tt1.start();
    tt2.start();

    execInitScript(tt1);
    execInitScript(tt2);

    count1 = ThreadLocalRandom.current().nextInt(MIN_CONNECTION_COUNT, MAX_CONNECTION_COUNT + 1);
    count2 = ThreadLocalRandom.current().nextInt(MIN_CONNECTION_COUNT, MAX_CONNECTION_COUNT + 1);
  }

  @AfterAll
  static void tearDown() {
    tt1.stop();
    tt2.stop();
  }

  private int getSessionCounter(TarantoolContainer<?> tt) throws Exception {
    List<?> result = executeCommandDecoded(tt, "return get_session_counter()");
    return (Integer) result.get(0);
  }

  private int getCallCounter(TarantoolContainer<?> tt) throws Exception {
    List<?> result = executeCommandDecoded(tt, "return get_call_counter()");
    return (Integer) result.get(0);
  }

  @Test
  public void testRoundRobin() throws Exception {
    IProtoClientPool pool = new IProtoClientPoolImpl(factory, timerResource);
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(tt1.getHost())
                .withPort(tt1.getFirstMappedPort())
                .withSize(count1)
                .withTag("node-a")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost(tt2.getHost())
                .withPort(tt2.getFirstMappedPort())
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

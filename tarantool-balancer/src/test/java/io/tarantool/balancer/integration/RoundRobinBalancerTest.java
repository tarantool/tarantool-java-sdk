/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.balancer.integration;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.msgpack.value.ValueFactory;
import org.testcontainers.containers.tarantool.Tarantool3Container;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
  private static final Tarantool3Container tt1 =
      new Tarantool3Container(DockerImageName.parse("tarantool/tarantool"), "test-node1")
          .withEnv(ENV_MAP);

  @Container
  private static final Tarantool3Container tt2 =
      new Tarantool3Container(DockerImageName.parse("tarantool/tarantool"), "test-node2")
          .withEnv(ENV_MAP);

  @BeforeAll
  public static void setUp() {
    count1 = ThreadLocalRandom.current().nextInt(MIN_CONNECTION_COUNT, MAX_CONNECTION_COUNT + 1);
    count2 = ThreadLocalRandom.current().nextInt(MIN_CONNECTION_COUNT, MAX_CONNECTION_COUNT + 1);
  }

  private int getSessionCounter(Tarantool3Container tt) throws Exception {
    return Integer.parseInt(tt.getExecResult("return get_session_counter()"));
  }

  private int getCallCounter(Tarantool3Container tt) throws Exception {
    return Integer.parseInt(tt.getExecResult("return get_call_counter()"));
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

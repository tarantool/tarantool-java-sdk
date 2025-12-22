/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.balancer.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.tarantool.core.ManagedResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_DATA;
import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.balancer.TarantoolDistributingRoundRobinBalancer;
import io.tarantool.balancer.exceptions.NoAvailableClientsException;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.IProtoClientPool;
import io.tarantool.pool.IProtoClientPoolImpl;
import io.tarantool.pool.InstanceConnectionGroup;


@Timeout(value = 15)
@Testcontainers
public class DistributingRoundRobinBalancerTest extends BaseTest {

  private static final Logger log = LoggerFactory.getLogger(DistributingRoundRobinBalancerTest.class);

  @Container
  private final TarantoolContainer tt1 = new TarantoolContainer()
      .withEnv(ENV_MAP)
      .withExposedPort(3305)
      .withLogConsumer(new Slf4jLogConsumer(log));

  @Container
  private final TarantoolContainer tt2 = new TarantoolContainer()
      .withEnv(ENV_MAP)
      .withExposedPort(3305)
      .withLogConsumer(new Slf4jLogConsumer(log));

  @BeforeEach
  public void setUp() {
    do {
      count1 = ThreadLocalRandom.current().nextInt(
          MIN_CONNECTION_COUNT,
          MAX_CONNECTION_COUNT + 1
      );
      count2 = ThreadLocalRandom.current().nextInt(
          MIN_CONNECTION_COUNT,
          MAX_CONNECTION_COUNT + 1
      );
    } while (count1 == count2);
  }

  private static IProtoClientPool createClientPool(boolean gracefulShutdown,
      HeartbeatOpts heartbeatOpts,
      MeterRegistry metricsRegistry) {
    ManagedResource<Timer> timerResource = ManagedResource.owned(new HashedWheelTimer(), Timer::stop);
    ConnectionFactory factory = new ConnectionFactory(bootstrap, timerResource.get());
    return new IProtoClientPoolImpl(
        factory, timerResource, gracefulShutdown, heartbeatOpts, null, metricsRegistry
    );
  }

  private int getSessionCounter(TarantoolContainer tt) throws Exception {
    List<?> result = tt.executeCommandDecoded("return get_session_counter()");
    return (Integer) result.get(0);
  }

  private int getCallCounter(TarantoolContainer tt) throws Exception {
    List<?> result = tt.executeCommandDecoded("return get_call_counter()");
    return (Integer) result.get(0);
  }

  private void execLua(TarantoolContainer container, String command) {
    try {
      container.executeCommandDecoded(command);
    } catch (Exception e) {
    }
  }

  private void wakeUpAllConnects(TarantoolBalancer rrBalancer,
      int nodeVisits,
      TarantoolContainer tt1,
      TarantoolContainer tt2) throws Exception {
    walkAndJoin(rrBalancer, nodeVisits * 2);
    assertEquals(count1, getSessionCounter(tt1));
    assertEquals(count2, getSessionCounter(tt2));
    assertEquals(nodeVisits, getCallCounter(tt1));
    assertEquals(nodeVisits, getCallCounter(tt2));
  }

  private void walkAndJoin(TarantoolBalancer balancer, int nodeVisits) {
    log.info("walkAndJoin: visits = {}", nodeVisits);
    List<CompletableFuture<IProtoResponse>> futures = new ArrayList<>();
    for (int i = 0; i < nodeVisits; i++) {
      CompletableFuture<IProtoResponse> future = balancer
          .getNext()
          .thenCompose(client -> {
            client.authorize(API_USER, CREDS.get(API_USER));
            return client.eval("inc()", emptyArgs);
          });
      futures.add(future);
    }
    CompletableFuture
        .allOf(futures.toArray(new CompletableFuture[0]))
        .join();
  }

  @Test
  public void testDistributingRoundRobin() throws Exception {
    log.info("BEGIN: testDistributingRoundRobin");
    IProtoClientPool pool = createClientPool(true, null, null);
    pool.setGroups(Arrays.asList(
        InstanceConnectionGroup.builder()
            .withHost(tt1.getHost())
            .withPort(tt1.getPort())
            .withSize(count1)
            .withTag("node-a-00")
            .build(),
        InstanceConnectionGroup.builder()
            .withHost(tt2.getHost())
            .withPort(tt2.getPort())
            .withSize(count2)
            .withTag("node-b-00")
            .build()));
    TarantoolBalancer rrBalancer = new TarantoolDistributingRoundRobinBalancer(pool);
    wakeUpAllConnects(rrBalancer, Integer.max(count1, count2), tt1, tt2);
    pool.close();
    log.info("END: testDistributingRoundRobin");
  }

  @Test
  public void testDistributingRoundRobinWithUnavailableNodeA() throws Exception {
    log.info("BEGIN: testDistributingRoundRobinWithUnavailableNodeA");
    IProtoClientPool pool = createClientPool(true, HEARTBEAT_OPTS, null);
    pool.setGroups(Arrays.asList(
        InstanceConnectionGroup.builder()
            .withHost(tt1.getHost())
            .withPort(tt1.getMappedPort(3305))
            .withSize(count1)
            .withTag("node-a-01")
            .build(),
        InstanceConnectionGroup.builder()
            .withHost(tt2.getHost())
            .withPort(tt2.getPort())
            .withSize(count2)
            .withTag("node-b-01")
            .build()));

    TarantoolBalancer rrBalancer = new TarantoolDistributingRoundRobinBalancer(pool);
    int nodeVisits = Integer.max(count1, count2);

    wakeUpAllConnects(rrBalancer, nodeVisits, tt1, tt2);
    Thread.sleep(RESTORE_TIMEOUT);

    log.info("lock pipe on node A");
    execLua(tt1, "lock_pipe(true); reset_call_counter()");
    execLua(tt2, "reset_call_counter()");

    log.info("waiting for invalidation");
    Thread.sleep(INVALIDATION_TIMEOUT);

    log.info("walk on node B only");
    walkAndJoin(rrBalancer, nodeVisits * 2);
    assertEquals(0, getCallCounter(tt1));
    assertEquals(nodeVisits * 2, getCallCounter(tt2));

    log.info("unlock pipe on node A");
    execLua(tt1, "lock_pipe(false); reset_call_counter()");
    execLua(tt2, "reset_call_counter()");

    log.info("waiting for restore");
    Thread.sleep(RESTORE_TIMEOUT);

    log.info("walk on all nodes");
    walkAndJoin(rrBalancer, nodeVisits * 2);

    int node1Visits = getCallCounter(tt1);
    int node2Visits = getCallCounter(tt2);
    assertEquals(nodeVisits * 2, node1Visits + node2Visits);
    assertTrue(node1Visits > count1 / 2);
    assertTrue(node2Visits > count2 / 2);

    pool.close();
    log.info("END: testDistributingRoundRobinWithUnavailableNodeA");
  }

  @Test
  public void testDistributingRoundRobinWithUnavailableNodeANoUnlock() throws Exception {
    log.info("BEGIN: testDistributingRoundRobinWithUnavailableNodeANoUnlock");
    IProtoClientPool pool = createClientPool(true, HEARTBEAT_OPTS, null);
    pool.setGroups(Arrays.asList(
        InstanceConnectionGroup.builder()
            .withHost(tt1.getHost())
            .withPort(tt1.getMappedPort(3305))
            .withSize(count1)
            .withTag("node-a-02")
            .build(),
        InstanceConnectionGroup.builder()
            .withHost(tt2.getHost())
            .withPort(tt2.getPort())
            .withSize(count2)
            .withTag("node-b-02")
            .build()));

    TarantoolBalancer rrBalancer = new TarantoolDistributingRoundRobinBalancer(pool);
    int nodeVisits = Integer.max(count1, count2);

    wakeUpAllConnects(rrBalancer, nodeVisits, tt1, tt2);
    Thread.sleep(RESTORE_TIMEOUT);

    log.info("lock pipe on node A");
    execLua(tt1, "lock_pipe(true); reset_call_counter()");
    execLua(tt2, "reset_call_counter()");

    log.info("waiting for invalidation");
    Thread.sleep(INVALIDATION_TIMEOUT);

    log.info("walk on node B only");
    walkAndJoin(rrBalancer, nodeVisits * 2);
    assertEquals(0, getCallCounter(tt1));
    assertEquals(nodeVisits * 2, getCallCounter(tt2));

    execLua(tt1, "reset_call_counter()");
    execLua(tt2, "reset_call_counter()");

    log.info("waiting for restore");
    Thread.sleep(RESTORE_TIMEOUT);

    log.info("walk on B node only");
    walkAndJoin(rrBalancer, nodeVisits * 2);

    int node1Visits = getCallCounter(tt1);
    int node2Visits = getCallCounter(tt2);
    assertEquals(nodeVisits * 2, node1Visits + node2Visits);
    assertEquals(0, node1Visits);
    assertEquals(nodeVisits * 2, node2Visits);

    pool.close();
    log.info("END: testDistributingRoundRobinWithUnavailableNodeANoUnlock");
  }

  @Test
  public void testDistributingRoundRobinNoAvailableClients() throws Exception {
    log.info("BEGIN: testDistributingRoundRobinNoAvailableClients");
    IProtoClientPool pool = createClientPool(true, HEARTBEAT_OPTS, null);
    pool.setGroups(Arrays.asList(
        InstanceConnectionGroup.builder()
            .withHost(tt1.getHost())
            .withPort(tt1.getMappedPort(3305))
            .withSize(count1)
            .withTag("node-a-03")
            .build(),
        InstanceConnectionGroup.builder()
            .withHost(tt2.getHost())
            .withPort(tt2.getMappedPort(3305))
            .withSize(count2)
            .withTag("node-b-03")
            .build()));
    TarantoolBalancer rrBalancer = new TarantoolDistributingRoundRobinBalancer(pool);
    int nodeVisits = Integer.max(count1, count2);

    wakeUpAllConnects(rrBalancer, nodeVisits, tt1, tt2);
    Thread.sleep(RESTORE_TIMEOUT);

    log.info("lock pipe on A and B");
    execLua(tt1, "lock_pipe(true); reset_call_counter()");
    execLua(tt2, "lock_pipe(true); reset_call_counter()");

    log.info("waiting for invalidation");
    Thread.sleep(INVALIDATION_TIMEOUT);

    Throwable exc = assertThrows(
        ExecutionException.class,
        () -> rrBalancer.getNext().get()
    );
    assertEquals(NoAvailableClientsException.class, exc.getCause().getClass());

    log.info("unlock pipe on A and B");
    execLua(tt1, "lock_pipe(false); reset_call_counter()");
    execLua(tt2, "lock_pipe(false); reset_call_counter()");

    log.info("waiting for restore");
    Thread.sleep(RESTORE_TIMEOUT);

    walkAndJoin(rrBalancer, nodeVisits * 2);

    int node1Visits = getCallCounter(tt1);
    int node2Visits = getCallCounter(tt2);
    assertEquals(nodeVisits * 2, node1Visits + node2Visits);
    assertTrue(node1Visits > count1 / 2);
    assertTrue(node2Visits > count2 / 2);

    pool.close();
    log.info("END: testDistributingRoundRobinNoAvailableClients");
  }

  @Test
  public void testDistributingRoundRobinStartWithStuckNodeA() throws Exception {
    log.info("BEGIN: testDistributingRoundRobinStartWithStuckNodeA");
    execLua(tt1, "stuck()");
    Thread.sleep(1000);

    IProtoClientPool pool = createClientPool(true, HEARTBEAT_OPTS, null);
    pool.setGroups(Arrays.asList(
        InstanceConnectionGroup.builder()
            .withHost(tt1.getHost())
            .withPort(tt1.getPort())
            .withTag("node-a-01")
            .build(),
        InstanceConnectionGroup.builder()
            .withHost(tt2.getHost())
            .withPort(tt2.getPort())
            .withTag("node-b-01")
            .build()));
    pool.setConnectTimeout(3_000);

    TarantoolBalancer rrBalancer = new TarantoolDistributingRoundRobinBalancer(pool);
    CompletableFuture<IProtoResponse> futureFirst = rrBalancer
        .getNext()
        .thenCompose(c -> {
          c.authorize(API_USER, CREDS.get(API_USER));
          return c.eval("return box.info.uuid", emptyArgs);
        });
    CompletableFuture<IProtoResponse> futureSecond = rrBalancer
        .getNext()
        .thenCompose(c -> {
          c.authorize(API_USER, CREDS.get(API_USER));
          return c.eval("return box.info.uuid", emptyArgs);
        });
    CompletableFuture.allOf(futureFirst, futureSecond).join();
    IProtoResponse resultFirst = futureFirst.join();
    IProtoResponse resultSecond = futureSecond.join();

    assertEquals(resultFirst.getBodyValue(IPROTO_DATA), resultSecond.getBodyValue(IPROTO_DATA));

    pool.close();
    log.info("END: testDistributingRoundRobinStartWithStuckNodeA");
  }
}

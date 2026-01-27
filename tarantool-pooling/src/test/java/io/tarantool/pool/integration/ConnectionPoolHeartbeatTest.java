/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.integration;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.tarantool.TarantoolContainerImpl;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.tarantool.core.IProtoClient;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.IProtoClientPool;

@Timeout(value = 60)
@Testcontainers
public class ConnectionPoolHeartbeatTest extends BasePoolTest {

  private static final Logger log = LoggerFactory.getLogger(ConnectionPoolHeartbeatTest.class);

  private static final int PING_INTERVAL = 500;
  private static final int WINDOW_SIZE = 4;
  private static final long TIMER_ERROR_MS = 100;
  private static final int INVALID_PINGS = 2;
  private static final int DEATH_THRESHOLD = 4;
  private static final long RESTORE_TIMEOUT = (WINDOW_SIZE + 1) * (PING_INTERVAL + TIMER_ERROR_MS);
  private static final HeartbeatOpts HEARTBEAT_OPTS =
      HeartbeatOpts.getDefault()
          .withPingInterval(PING_INTERVAL)
          .withInvalidationThreshold(INVALID_PINGS)
          .withWindowSize(WINDOW_SIZE)
          .withDeathThreshold(DEATH_THRESHOLD);
  private static final HeartbeatOpts CRUD_HEARTBEAT_OPTS =
      HeartbeatOpts.getDefault()
          .withPingInterval(PING_INTERVAL)
          .withInvalidationThreshold(INVALID_PINGS)
          .withWindowSize(WINDOW_SIZE)
          .withDeathThreshold(DEATH_THRESHOLD)
          .withCrudHealthCheck();

  @Container
  private static final TarantoolContainerImpl tt1 =
      new TarantoolContainerImpl().withEnv(ENV_MAP).withExposedPort(3305);

  @Container
  private static final TarantoolContainerImpl tt2 =
      new TarantoolContainerImpl().withEnv(ENV_MAP).withExposedPort(3305);

  @BeforeEach
  public void setUp() {
    host1 = tt1.getHost();
    port1 = tt1.getMappedPort(3305);
    host2 = tt2.getHost();
    port2 = tt2.getMappedPort(3305);
    generateCounts();
  }

  @Test
  public void testConnectInvalidationAndRestore() throws Exception {
    MeterRegistry metricsRegistry = createMetricsRegistry();
    IProtoClientPool pool = createPool(HEARTBEAT_OPTS, metricsRegistry);
    assertTrue(pool.hasAvailableClients());
    assertEquals(count1 + count2, metricsRegistry.get("pool.size").gauge().value());
    assertEquals(count1 + count2, metricsRegistry.get("pool.available").gauge().value());

    List<IProtoClient> clientsA = getConnects(pool, "node-a", count1);
    List<IProtoClient> clientsB = getConnects(pool, "node-b", count2);

    assertEquals(count1 + count2, metricsRegistry.get("pool.connect.success").counter().count());
    assertEquals(count1 + count2, metricsRegistry.get("pool.request.success").counter().count());
    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    assertTrue(pingClients(clientsA));
    assertTrue(pingClients(clientsB));

    execLua(tt1, "lock_pipe(true)");

    waitFor(
        "Connections are still available",
        Duration.ofSeconds(10),
        () -> {
          assertTrue(metricsRegistry.get("pool.heartbeat.success").counter().count() > 0);
          assertTrue(metricsRegistry.get("pool.heartbeat.errors").counter().count() > 0);
          assertEquals(count1, metricsRegistry.get("pool.invalidated").gauge().value());
          assertEquals(count2, metricsRegistry.get("pool.available").gauge().value());

          assertTrue(pool.hasAvailableClients());

          for (int i = 0; i < count1; i++) {
            assertNull(pool.get("node-a", i));
          }
          for (int i = 0; i < count2; i++) {
            assertNotNull(pool.get("node-b", i));
          }
          assertEquals(count1, metricsRegistry.get("pool.request.unavail").counter().count());
          assertEquals(
              count1 + 2 * count2, metricsRegistry.get("pool.request.success").counter().count());

          assertEquals(count1, getActiveConnectionsCount(tt1));
          assertEquals(count2, getActiveConnectionsCount(tt2));
        });

    execLua(tt1, "lock_pipe(false)");

    waitFor(
        "Connections are still not restored",
        Duration.ofSeconds(10),
        () -> {
          assertTrue(pool.hasAvailableClients());
          assertEquals(count1 + count2, metricsRegistry.get("pool.available").gauge().value());

          for (int i = 0; i < count1; i++) {
            assertNotNull(pool.get("node-a", i));
          }
          for (int i = 0; i < count2; i++) {
            assertNotNull(pool.get("node-b", i));
          }
          assertEquals(
              2 * count1 + 3 * count2,
              metricsRegistry.get("pool.request.success").counter().count());
          assertEquals(0, metricsRegistry.get("pool.invalidated").gauge().value());

          assertEquals(count1, getActiveConnectionsCount(tt1));
          assertEquals(count2, getActiveConnectionsCount(tt2));
        });

    pool.close();
  }

  @Test
  public void testConnectNoAvail() throws Exception {
    MeterRegistry metricsRegistry = createMetricsRegistry();
    IProtoClientPool pool = createPool(HEARTBEAT_OPTS, metricsRegistry);
    assertTrue(pool.hasAvailableClients());

    List<IProtoClient> clientsA = getConnects(pool, "node-a", count1);
    List<IProtoClient> clientsB = getConnects(pool, "node-b", count2);

    assertTrue(pingClients(clientsA));
    assertTrue(pingClients(clientsB));

    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    execLua(tt1, "lock_pipe(true)");
    execLua(tt2, "lock_pipe(true)");

    waitFor(
        "Connections are still available",
        Duration.ofSeconds(10),
        () -> {
          assertFalse(pool.hasAvailableClients());

          for (int i = 0; i < count1; i++) {
            assertNull(pool.get("node-a", i));
          }
          for (int i = 0; i < count2; i++) {
            assertNull(pool.get("node-b", i));
          }

          assertEquals(count1, getActiveConnectionsCount(tt1));
          assertEquals(count2, getActiveConnectionsCount(tt2));
        });

    execLua(tt1, "lock_pipe(false)");
    execLua(tt2, "lock_pipe(false)");

    waitFor(
        "Connections are still not restored",
        Duration.ofSeconds(10),
        () -> {
          assertTrue(pool.hasAvailableClients());

          for (int i = 0; i < count1; i++) {
            assertNotNull(pool.get("node-a", i));
          }
          for (int i = 0; i < count2; i++) {
            assertNotNull(pool.get("node-b", i));
          }
        });

    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    pool.close();
  }

  @Test
  public void testConnectNoAvailWithCrudHeartbeat() throws Exception {
    execLua(tt1, "rawset(_G, 'crud', {})");
    execLua(tt2, "rawset(_G, 'crud', {})");

    MeterRegistry metricsRegistry = createMetricsRegistry();
    IProtoClientPool pool = createPool(CRUD_HEARTBEAT_OPTS, metricsRegistry);
    assertTrue(pool.hasAvailableClients());

    List<IProtoClient> clientsA = getConnects(pool, "node-a", count1);
    List<IProtoClient> clientsB = getConnects(pool, "node-b", count2);

    assertTrue(pingClients(clientsA));
    assertTrue(pingClients(clientsB));

    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    execLua(tt1, "rawset(_G, 'tmp_crud', crud); rawset(_G, 'crud', nil)");
    execLua(tt2, "rawset(_G, 'tmp_crud', crud); rawset(_G, 'crud', nil)");

    waitFor(
        "Connections are still alive after breaking crud",
        Duration.ofSeconds(10),
        () -> {
          assertFalse(pool.hasAvailableClients());
          for (int i = 0; i < count1; i++) {
            assertNull(pool.get("node-a", i));
          }
          for (int i = 0; i < count2; i++) {
            assertNull(pool.get("node-b", i));
          }
        });

    execLua(tt1, "rawset(_G, 'crud', tmp_crud)");
    execLua(tt2, "rawset(_G, 'crud', tmp_crud)");

    waitFor(
        "Connections are still broken after fixing crud",
        Duration.ofSeconds(10),
        () -> {
          assertTrue(pool.hasAvailableClients());
          for (int i = 0; i < count1; i++) {
            assertNotNull(pool.get("node-a", i));
          }
          for (int i = 0; i < count2; i++) {
            assertNotNull(pool.get("node-b", i));
          }
          assertEquals(count1, getActiveConnectionsCount(tt1));
          assertEquals(count2, getActiveConnectionsCount(tt2));
        });

    pool.close();
  }

  @Test
  public void testReconnectsAfterInvalidationAndKill() throws Exception {
    MeterRegistry metricsRegistry = createMetricsRegistry();
    IProtoClientPool pool = createPool(HEARTBEAT_OPTS, metricsRegistry);
    assertTrue(pool.hasAvailableClients());

    List<IProtoClient> clientsA = getConnects(pool, "node-a", count1);
    List<IProtoClient> clientsB = getConnects(pool, "node-b", count2);

    Runnable nodeAConnectsAreNotAvailable =
        () -> {
          for (int i = 0; i < count1; i++) {
            assertNull(pool.get("node-a", i));
          }
        };

    assertTrue(pingClients(clientsA));
    assertTrue(pingClients(clientsB));

    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    log.info("lock pipe on A");
    execLua(tt1, "lock_pipe(true)");

    log.info("Wait for invalidation connects to A");
    waitFor("No connect for A is invalidated", Duration.ofMinutes(1), nodeAConnectsAreNotAvailable);
    assertTrue(pool.hasAvailableClients());

    log.info("check: A is invalidated, B is ok");
    for (int i = 0; i < count2; i++) {
      assertNotNull(pool.get("node-b", i));
    }

    log.info("check: A is invalidated, but has active connects");
    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    log.info("check: A has no active connects");
    waitFor(
        "Not all connects to node A closed",
        Duration.ofMinutes(1),
        () -> assertEquals(0, getActiveConnectionsCount(tt1)));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    Thread.sleep(RESTORE_TIMEOUT * 2);

    // note: actually on node A can be active connects when client connects
    // and tries to do ping request, but after ping check theese connect
    // will be closes imediately
    log.info("check: A still has no active connects");
    waitFor("No connect for A is invalidated", Duration.ofMinutes(1), nodeAConnectsAreNotAvailable);
    waitFor(
        "Not all connects to node A closed",
        Duration.ofMinutes(1),
        () -> assertEquals(0, getActiveConnectionsCount(tt1)));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    log.info("unlock pipe on A");
    execLua(tt1, "lock_pipe(false)");

    waitFor(
        "Not all connects to node A restored",
        Duration.ofMinutes(1),
        () -> {
          for (int i = 0; i < count1; i++) {
            assertNotNull(pool.get("node-a", i));
          }
        });
    assertTrue(pool.hasAvailableClients());

    for (int i = 0; i < count2; i++) {
      assertNotNull(pool.get("node-b", i));
    }

    log.info("check: A and B nodes have connections");
    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    pool.close();
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.integration;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.utils.TarantoolContainerClientHelper;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.pool.IProtoClientPool;
import io.tarantool.pool.IProtoClientPoolImpl;
import io.tarantool.pool.InstanceConnectionGroup;

@Timeout(value = 120)
public class ConnectionPoolReconnectsTest extends BasePoolTest {

  private static TarantoolContainer<?> tt;

  @BeforeAll
  static void beforeAll() {
    tt =
        TarantoolContainerClientHelper.createTarantoolContainer()
            .withEnv(ENV_MAP)
            .withFixedExposedPort(3301, 3301);
    tt.start();
    TarantoolContainerClientHelper.execInitScript(tt);
  }

  @BeforeEach
  public void setUp() {
    generateCounts();
  }

  @AfterAll
  static void tearDown() {
    tt.stop();
  }

  @Test
  public void testReconnectAfterNodeFailure() throws Exception {
    MeterRegistry metricsRegistry = createMetricsRegistry();
    ManagedResource<Timer> timerResource =
        ManagedResource.owned(new HashedWheelTimer(), Timer::stop);
    ConnectionFactory factory = new ConnectionFactory(bootstrap, timerResource.get());
    IProtoClientPool pool =
        new IProtoClientPoolImpl(factory, timerResource, true, null, null, metricsRegistry);
    pool.setGroups(
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost(tt.getHost())
                .withSize(count1)
                .withTag("node-a")
                .build()));
    pool.setConnectTimeout(1_000L);

    assertTrue(pool.hasAvailableClients());
    List<IProtoClient> clients = getConnects(pool, "node-a", count1);
    assertTrue(pingClients(clients));
    assertEquals(count1, getActiveConnectionsCount(tt));

    tt.stop();
    Thread.sleep(1000);

    assertFalse(pool.hasAvailableClients());
    for (int i = 0; i < count1; i++) {
      assertNull(pool.get("node-a", i));
    }

    assertTrue(metricsRegistry.get("pool.reconnecting").gauge().value() > 0);

    tt =
        TarantoolContainerClientHelper.createTarantoolContainer()
            .withEnv(ENV_MAP)
            .withFixedExposedPort(3301, 3301);
    tt.start();
    TarantoolContainerClientHelper.execInitScript(tt);

    waitFor(
        "No available connects",
        Duration.ofMinutes(1),
        () -> assertTrue(pool.hasAvailableClients()));
    waitFor(
        "Not all connects established",
        Duration.ofMinutes(3),
        () -> {
          for (int i = 0; i < count1; i++) {
            assertNotNull(pool.get("node-a", i));
          }
        });

    assertTrue(pingClients(clients));
    assertEquals(count1, getActiveConnectionsCount(tt));

    assertEquals(count1, metricsRegistry.get("pool.size").gauge().value());
    assertEquals(count1, metricsRegistry.get("pool.available").gauge().value());
    assertEquals(0, metricsRegistry.get("pool.reconnecting").gauge().value());
    assertEquals(0, metricsRegistry.get("pool.invalidated").gauge().value());
    assertTrue(metricsRegistry.get("pool.connect.errors").counter().count() > 0);
    assertTrue(metricsRegistry.get("pool.connect.success").counter().count() > 0);

    pool.close();
  }
}

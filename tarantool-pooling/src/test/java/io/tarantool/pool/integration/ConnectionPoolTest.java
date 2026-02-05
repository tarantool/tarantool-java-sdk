/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.containers.utils.TarantoolContainerClientHelper.createTarantoolContainer;
import static org.testcontainers.containers.utils.TarantoolContainerClientHelper.execInitScript;
import static org.testcontainers.containers.utils.TarantoolContainerClientHelper.executeCommandDecoded;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.ValueFactory;
import org.testcontainers.containers.tarantool.TarantoolContainer;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.core.connection.exceptions.ConnectionException;
import io.tarantool.core.protocol.BoxIterator;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatEvent;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.IProtoClientPool;
import io.tarantool.pool.IProtoClientPoolImpl;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEventListener;
import io.tarantool.pool.TripleConsumer;
import io.tarantool.pool.exceptions.PoolClosedException;

@Timeout(value = 5)
public class ConnectionPoolTest extends BasePoolTest {

  protected static final Bootstrap bootstrap =
      new Bootstrap()
          .group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
          .channel(NioSocketChannel.class)
          .option(ChannelOption.SO_REUSEADDR, true)
          .option(ChannelOption.SO_KEEPALIVE, true)
          .option(ChannelOption.TCP_NODELAY, true)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

  private static final int MAX_CONNECTION_COUNT = 20;
  private static final int MIN_CONNECTION_COUNT = 10;

  private static String host1;
  private static int port1;
  private static int count1;
  private static String host2;
  private static int port2;
  private static int count2;

  private static TarantoolContainer<?> tt1;
  private static TarantoolContainer<?> tt2;

  @BeforeAll
  public static void setUp() {
    tt1 = createTarantoolContainer().withEnv(ENV_MAP);
    tt2 = createTarantoolContainer().withEnv(ENV_MAP);
    tt1.start();
    tt2.start();
    execInitScript(tt1);
    execInitScript(tt2);

    host1 = tt1.getHost();
    port1 = tt1.getFirstMappedPort();
    count1 = ThreadLocalRandom.current().nextInt(MIN_CONNECTION_COUNT, MAX_CONNECTION_COUNT + 1);
    host2 = tt2.getHost();
    port2 = tt2.getFirstMappedPort();
    count2 = ThreadLocalRandom.current().nextInt(MIN_CONNECTION_COUNT, MAX_CONNECTION_COUNT + 1);
  }

  @AfterAll
  static void tearDown() {
    tt1.stop();
    tt2.stop();
  }

  @Test
  public void testConnect() throws Exception {
    IProtoClientPool pool = createClientPool(true, null);
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(count1)
                .withTag("node-a")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost(host2)
                .withPort(port2)
                .withSize(count2)
                .withTag("node-b")
                .build()));

    List<IProtoClient> clients = new ArrayList<>();
    for (int i = 0; i < count1; i++) {
      clients.add(pool.get("node-a", i).get());
    }
    for (int i = 0; i < count2; i++) {
      clients.add(pool.get("node-b", i).get());
    }
    for (IProtoClient client : clients) {
      assertTrue(client.isConnected());
      client.ping().get();
    }
    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));
    pool.close();
  }

  private static IProtoClientPool createClientPool(
      boolean gracefulShutdown, MeterRegistry metricsRegistry) {
    return createClientPool(gracefulShutdown, metricsRegistry, null, null, null);
  }

  private static IProtoClientPool createClientPool(
      boolean gracefulShutdown,
      MeterRegistry metricsRegistry,
      TripleConsumer<String, Integer, IProtoResponse> ignoredCatcher) {
    return createClientPool(gracefulShutdown, metricsRegistry, ignoredCatcher, null, null);
  }

  private static IProtoClientPool createClientPool(
      boolean gracefulShutdown,
      MeterRegistry metricsRegistry,
      TripleConsumer<String, Integer, IProtoResponse> ignoredCatcher,
      PoolEventListener poolEventListener,
      HeartbeatOpts heartbeatOpts) {
    ManagedResource<Timer> timerResource =
        ManagedResource.owned(new HashedWheelTimer(), Timer::stop);
    ConnectionFactory factory = new ConnectionFactory(bootstrap, timerResource.get());
    return new IProtoClientPoolImpl(
        factory,
        timerResource,
        gracefulShutdown,
        heartbeatOpts,
        null,
        metricsRegistry,
        ignoredCatcher,
        false,
        poolEventListener);
  }

  @Test
  public void testDoubleConnect() throws Exception {
    IProtoClientPool pool = createClientPool(true, null);
    pool.setGroups(
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withTag("node-a")
                .build()));

    CompletableFuture<IProtoClient> future1 = pool.get("node-a", 0);
    CompletableFuture<IProtoClient> future2 = pool.get("node-a", 0);
    CompletableFuture.allOf(future1, future2).join();
    assertTrue(future1 == future2);
    assertEquals(1, getActiveConnectionsCount(tt1));
    pool.close();
  }

  @Test
  public void testConnectWithCredentials() throws Exception {
    IProtoClientPool pool = createClientPool(true, null);
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(count1)
                .withTag("node-a")
                .withUser("user_a")
                .withPassword("secret_a")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost(host2)
                .withPort(port2)
                .withSize(count2)
                .withTag("node-b")
                .withUser("user_b")
                .withPassword("secret_b")
                .build()));

    List<IProtoClient> clients = new ArrayList<>();
    for (int i = 0; i < count1; i++) {
      clients.add(pool.get("node-a", i).get());
    }
    for (int i = 0; i < count2; i++) {
      clients.add(pool.get("node-b", i).get());
    }
    List<?> result = executeCommandDecoded(tt1, "return box.space.space_a.id");
    Integer space = (Integer) result.get(0);
    for (IProtoClient client : clients) {
      assertTrue(client.isConnected());
      client
          .select(
              space,
              0,
              ValueFactory.newArray(ValueFactory.newString("testkey")),
              1,
              0,
              BoxIterator.GE)
          .get();
    }
    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));
    pool.close();
  }

  @Test
  public void testPoolEventListenerReceivesLifecycleEvents() throws Exception {
    RecordingPoolEventListener listener = new RecordingPoolEventListener();
    HeartbeatOpts heartbeatOpts =
        HeartbeatOpts.getDefault()
            .withPingInterval(100)
            .withWindowSize(1)
            .withInvalidationThreshold(1)
            .withDeathThreshold(2);
    IProtoClientPool pool = createClientPool(true, null, null, listener, heartbeatOpts);
    pool.setGroups(
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(1)
                .withTag("node-events")
                .withUser("user_a")
                .withPassword("secret_a")
                .build()));

    IProtoClient client = pool.get("node-events", 0).get();
    client.ping().get();
    TimeUnit.MILLISECONDS.sleep(heartbeatOpts.getPingInterval() * 2);

    assertTrue(listener.getConnectionOpened().contains("node-events:0"));
    assertTrue(listener.getHeartbeatEventsFor("node-events:0").contains(HeartbeatEvent.ACTIVATE));

    pool.close();
    assertTrue(listener.getConnectionClosed().contains("node-events:0"));
  }

  @Test
  public void testPoolEventListenerRecordsReconnectSchedules() throws Exception {
    RecordingPoolEventListener listener = new RecordingPoolEventListener();
    IProtoClientPool pool = createClientPool(true, null, null, listener, null);
    pool.setConnectTimeout(200);
    pool.setGroups(
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(65_001)
                .withSize(1)
                .withTag("node-events")
                .build()));

    listener.expectReconnect("node-events:0");
    assertThrows(ExecutionException.class, () -> pool.get("node-events", 0).get());
    assertTrue(
        listener.awaitReconnect("node-events:0", pool.getReconnectAfter() + 2_000),
        "Reconnect event not received in time");
    List<Long> reconnectDelays = listener.getReconnectDelaysFor("node-events:0");
    assertFalse(reconnectDelays.isEmpty());
    assertTrue(reconnectDelays.get(0) >= pool.getReconnectAfter());

    pool.close();
  }

  @Test
  public void testConnectWithBadCredentials() throws Exception {
    IProtoClientPool pool = createClientPool(true, null);
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(count1)
                .withTag("node-a")
                .withUser("user_a")
                .withPassword("secret")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost(host2)
                .withPort(port2)
                .withSize(count2)
                .withTag("node-b")
                .withUser("user_b")
                .withPassword("secret")
                .build()));

    for (int i = 0; i < count1; i++) {
      final int j = i; // because lambda requires that
      // variables should be final
      assertThrows(ExecutionException.class, () -> pool.get("node-a", j).get());
    }
    for (int i = 0; i < count2; i++) {
      final int j = i; // because lambda requires that
      // variables should be final
      assertThrows(ExecutionException.class, () -> pool.get("node-b", j).get());
    }
    assertEquals(0, getActiveConnectionsCount(tt1));
    assertEquals(0, getActiveConnectionsCount(tt2));
    pool.close();
  }

  @Test
  public void testConnectErrorAfterPoolClose() throws Exception {
    IProtoClientPool pool = createClientPool(true, null);
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(count1)
                .withTag("node-a")
                .build()));

    List<IProtoClient> clients = new ArrayList<>();
    for (int i = 0; i < count1; i++) {
      clients.add(pool.get("node-a", i).get());
    }
    for (IProtoClient client : clients) {
      assertTrue(client.isConnected());
      client.ping().get();
    }
    assertEquals(count1, getActiveConnectionsCount(tt1));
    pool.close();
    for (int i = 0; i < count1; i++) {
      final int j = i; // because lambda requires that
      // variables should be final
      assertThrows(PoolClosedException.class, () -> pool.get("node-a", j));
    }
  }

  @Test
  public void testConnectWithChangeGroups() throws Exception {
    List<IProtoClient> clients = new ArrayList<IProtoClient>();
    IProtoClientPool pool = createClientPool(true, null);
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(count1)
                .withTag("node-a")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost(host2)
                .withPort(port2)
                .withSize(count2)
                .withTag("node-b")
                .build()));

    for (int i = 0; i < count1; i++) {
      clients.add(pool.get("node-a", i).get());
    }
    for (int i = 0; i < count2; i++) {
      clients.add(pool.get("node-b", i).get());
    }
    for (IProtoClient client : clients) {
      assertTrue(client.isConnected());
      client.ping().get();
    }
    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    clients.clear();
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(count1 / 2)
                .withTag("node-a")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost(host2)
                .withPort(port2)
                .withSize(count2 * 2)
                .withTag("node-b")
                .build()));

    for (int i = 0; i < count1 / 2; i++) {
      clients.add(pool.get("node-a", i).get());
    }
    for (int i = 0; i < count2 * 2; i++) {
      clients.add(pool.get("node-b", i).get());
    }
    for (IProtoClient client : clients) {
      assertTrue(client.isConnected());
      client.ping().get();
    }
    assertEquals(count1 / 2, getActiveConnectionsCount(tt1));
    assertEquals(count2 * 2, getActiveConnectionsCount(tt2));

    clients.clear();
    pool.setGroups(
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withTag("node-a")
                .build()));

    clients.add(pool.get("node-a", 0).get());
    for (IProtoClient client : clients) {
      assertTrue(client.isConnected());
      client.ping().get();
    }
    assertEquals(1, getActiveConnectionsCount(tt1));
    assertEquals(0, getActiveConnectionsCount(tt2));

    pool.close();
  }

  @Test
  public void testConnectWithChangeGroupHosts() throws Exception {
    List<IProtoClient> clients = new ArrayList<IProtoClient>();
    IProtoClientPool pool = createClientPool(true, null);
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(count1)
                .withTag("node-a")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost(host2)
                .withPort(port2)
                .withSize(count2)
                .withTag("node-b")
                .build()));

    for (int i = 0; i < count1; i++) {
      clients.add(pool.get("node-a", i).get());
    }
    for (int i = 0; i < count2; i++) {
      clients.add(pool.get("node-b", i).get());
    }
    for (IProtoClient client : clients) {
      assertTrue(client.isConnected());
      client.authorize(API_USER, CREDS.get(API_USER)).join();
      client.ping().get();
    }
    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    clients.clear();
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(host2)
                .withPort(port2)
                .withSize(count2)
                .withTag("node-a")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(count1)
                .withTag("node-b")
                .build()));

    for (int i = 0; i < count2; i++) {
      clients.add(pool.get("node-a", i).get());
    }
    for (int i = 0; i < count1; i++) {
      clients.add(pool.get("node-b", i).get());
    }
    for (IProtoClient client : clients) {
      assertTrue(client.isConnected());
      client.ping().get();
    }
    assertThrows(IndexOutOfBoundsException.class, () -> pool.get("node-a", count2));
    assertThrows(IndexOutOfBoundsException.class, () -> pool.get("node-b", count1));
    assertEquals(count1, getActiveConnectionsCount(tt1));
    assertEquals(count2, getActiveConnectionsCount(tt2));

    pool.close();
  }

  @Test
  public void testGetConnectOutOfPool() throws Exception {
    MeterRegistry metricsRegistry = createMetricsRegistry();
    IProtoClientPool pool = createClientPool(true, metricsRegistry);
    pool.setGroups(
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(count1)
                .withTag("node-a")
                .build()));

    assertThrows(NoSuchElementException.class, () -> pool.get("node-b", 0).get());
    assertThrows(IndexOutOfBoundsException.class, () -> pool.get("node-a", count1));
    // since we didn't request connection from pool, no connections are
    // created actually
    assertEquals(0, getActiveConnectionsCount(tt1));
    assertEquals(0, getActiveConnectionsCount(tt2));
    pool.close();
    assertTrue(metricsRegistry.get("pool.request.misses").counter().count() > 0);
  }

  @Test
  public void testConnectError() throws Exception {
    MeterRegistry metricsRegistry = createMetricsRegistry();
    IProtoClientPool pool = createClientPool(true, metricsRegistry);
    pool.setGroups(
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(10)
                .withSize(count1)
                .withTag("node-a")
                .build()));

    Exception ex = assertThrows(ExecutionException.class, () -> pool.get("node-a", 0).get());
    Throwable cause = ex.getCause();
    assertEquals(ConnectionException.class, cause.getClass());
    pool.close();

    assertTrue(metricsRegistry.get("pool.request.errors").counter().count() > 0);
  }

  @Test
  public void testRequestsWithIgnoredPacketsHandler() throws Exception {
    List<List<Object>> triplets = new ArrayList<>();
    TripleConsumer<String, Integer, IProtoResponse> ignoredCatcher =
        (tag, index, packet) -> {
          synchronized (triplets) {
            triplets.add(Arrays.asList(tag, index, packet));
          }
        };
    IProtoClientPool pool = createClientPool(true, null, ignoredCatcher);
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(1)
                .withTag("node-a")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost(host2)
                .withPort(port2)
                .withSize(1)
                .withTag("node-b")
                .build()));

    IProtoRequestOpts opts = IProtoRequestOpts.empty().withRequestTimeout(1000);
    ArrayValue args =
        ValueFactory.newArray(ValueFactory.newString("one"), ValueFactory.newInteger(1));
    List<CompletableFuture<IProtoResponse>> futures = new ArrayList<>();

    IProtoClient clientA = pool.get("node-a", 0).get();
    IProtoClient clientB = pool.get("node-b", 0).get();

    clientA.authorize(API_USER, CREDS.get(API_USER)).join();
    clientB.authorize(API_USER, CREDS.get(API_USER)).join();

    futures.add(
        pool.get("node-a", 0)
            .thenApply(client -> client.call("slow_echo", args, opts))
            .thenCompose(f -> f));
    futures.add(
        pool.get("node-b", 0)
            .thenApply(client -> client.call("slow_echo", args, opts))
            .thenCompose(f -> f));
    for (CompletableFuture<IProtoResponse> future : futures) {
      Exception ex = assertThrows(CompletionException.class, future::join);
      Throwable cause = ex.getCause();
      assertEquals(TimeoutException.class, cause.getClass());
    }
    Thread.sleep(600);
    assertEquals(2, triplets.size());
    Set<String> tags = new HashSet<>();
    Set<Integer> indexes = new HashSet<>();

    for (List<Object> item : triplets) {
      tags.add((String) item.get(0));
      indexes.add((int) item.get(1));
      assertTrue(item.get(2) instanceof IProtoResponse);
    }

    assertEquals(new HashSet<String>(Arrays.asList("node-a", "node-b")), tags);
    assertEquals(Collections.singleton(0), indexes);

    pool.close();
  }

  private static final class RecordingPoolEventListener implements PoolEventListener {
    private final List<String> connectionOpened = new CopyOnWriteArrayList<>();
    private final List<String> connectionClosed = new CopyOnWriteArrayList<>();
    private final Map<String, ConnectionEvents> eventsByConnection = new ConcurrentHashMap<>();
    private final Map<String, CountDownLatch> reconnectAwaiters = new ConcurrentHashMap<>();

    @Override
    public void onConnectionOpened(String tag, int index) {
      connectionOpened.add(tag + ":" + index);
    }

    @Override
    public void onConnectionClosed(String tag, int index) {
      connectionClosed.add(tag + ":" + index);
    }

    @Override
    public void onHeartbeatEvent(String tag, int index, HeartbeatEvent event) {
      eventsByConnection
          .computeIfAbsent(tag + ":" + index, k -> new ConnectionEvents())
          .heartbeatEvents
          .add(event);
    }

    @Override
    public void onReconnectScheduled(String tag, int index, long delayMs) {
      eventsByConnection
          .computeIfAbsent(tag + ":" + index, k -> new ConnectionEvents())
          .reconnectDelays
          .add(delayMs);
      CountDownLatch latch = reconnectAwaiters.get(tag + ":" + index);
      if (latch != null) {
        latch.countDown();
      }
    }

    List<String> getConnectionOpened() {
      return connectionOpened;
    }

    List<String> getConnectionClosed() {
      return connectionClosed;
    }

    List<HeartbeatEvent> getHeartbeatEventsFor(String connectionId) {
      ConnectionEvents events = eventsByConnection.get(connectionId);
      if (events == null) {
        return Collections.emptyList();
      }
      return events.heartbeatEvents;
    }

    List<Long> getReconnectDelaysFor(String connectionId) {
      ConnectionEvents events = eventsByConnection.get(connectionId);
      if (events == null) {
        return Collections.emptyList();
      }
      return events.reconnectDelays;
    }

    void expectReconnect(String connectionId) {
      reconnectAwaiters.put(connectionId, new CountDownLatch(1));
    }

    boolean awaitReconnect(String connectionId, long timeoutMs) throws InterruptedException {
      CountDownLatch latch =
          reconnectAwaiters.computeIfAbsent(connectionId, k -> new CountDownLatch(1));
      return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }
  }

  private static final class ConnectionEvents {
    private final List<HeartbeatEvent> heartbeatEvents = new CopyOnWriteArrayList<>();
    private final List<Long> reconnectDelays = new CopyOnWriteArrayList<>();
  }
}

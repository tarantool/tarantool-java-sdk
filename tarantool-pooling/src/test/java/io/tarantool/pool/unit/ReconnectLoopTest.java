/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.unit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.tarantool.core.ManagedResource;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.pool.IProtoClientPoolImpl;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEventListener;

/** A pool entry keeps reconnecting to a server that accepts connections and never answers. */
public class ReconnectLoopTest {

  private static final String TAG = "silent";
  private static final long CONNECT_TIMEOUT_MS = 200;
  private static final long RECONNECT_AFTER_MS = 50;
  private static final long OBSERVE_MS = 3_000;

  private EventLoopGroup group;
  private Timer timer;
  private ServerSocket server;
  private Thread acceptor;
  private final List<Socket> accepted = Collections.synchronizedList(new ArrayList<>());
  private IProtoClientPoolImpl pool;

  @BeforeEach
  public void setUp() throws IOException {
    group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    timer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS);
    server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
    // Accepts connections and never writes the greeting; each accepted socket is one attempt.
    acceptor =
        new Thread(
            () -> {
              while (!server.isClosed()) {
                try {
                  accepted.add(server.accept());
                } catch (IOException e) {
                  return;
                }
              }
            });
    acceptor.setDaemon(true);
    acceptor.start();
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (pool != null) {
      pool.close();
    }
    server.close();
    synchronized (accepted) {
      for (Socket socket : accepted) {
        socket.close();
      }
    }
    timer.stop();
    group.shutdownGracefully().get();
  }

  @Test
  public void test_failedAttempts_shouldNotMultiplyReconnectTasks() throws Exception {
    AtomicInteger scheduled = new AtomicInteger();
    PoolEventListener listener =
        new PoolEventListener() {
          @Override
          public void onReconnectScheduled(String tag, int index, long delayMs) {
            scheduled.incrementAndGet();
          }
        };
    pool = createPool(null, listener);
    pool.get(TAG, 0);

    Thread.sleep(OBSERVE_MS);

    int attempts = accepted.size();
    assertTrue(attempts >= 3, "too few attempts to judge: " + attempts);
    // Each failed attempt schedules one reconnect. When a greeting timeout was also reported as
    // a close by server, every attempt was handled twice and the reconnect chains multiplied.
    assertTrue(
        scheduled.get() <= attempts + 1,
        "reconnect tasks multiply: "
            + scheduled.get()
            + " scheduled for "
            + attempts
            + " attempts");
  }

  @Test
  public void test_closedBeforeGreeting_shouldNotMultiplyReconnectTasks() throws Exception {
    AtomicInteger scheduled = new AtomicInteger();
    PoolEventListener listener =
        new PoolEventListener() {
          @Override
          public void onReconnectScheduled(String tag, int index, long delayMs) {
            scheduled.incrementAndGet();
          }
        };
    AtomicInteger attempts = new AtomicInteger();
    try (ServerSocket closingServer = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
      // Accepts and closes at once, before the greeting, like a proxy without a backend.
      Thread closer =
          new Thread(
              () -> {
                while (!closingServer.isClosed()) {
                  try (Socket socket = closingServer.accept()) {
                    attempts.incrementAndGet();
                  } catch (IOException e) {
                    return;
                  }
                }
              });
      closer.setDaemon(true);
      closer.start();
      pool = createPool(null, listener, closingServer);
      pool.get(TAG, 0);

      Thread.sleep(OBSERVE_MS);
    }

    int count = attempts.get();
    assertTrue(count >= 3, "too few attempts to judge: " + count);
    // A connection closed before the greeting has never been established: the failed connect
    // future is the only report of the attempt, so each attempt schedules one reconnect.
    assertTrue(
        scheduled.get() <= count + 1,
        "reconnect tasks multiply: " + scheduled.get() + " scheduled for " + count + " attempts");
  }

  @Test
  public void test_failedAttempts_shouldStopConnectTimer() throws Exception {
    MeterRegistry registry = createMetricsRegistry();
    pool = createPool(registry, null);
    pool.get(TAG, 0);

    Thread.sleep(OBSERVE_MS);

    int attempts = accepted.size();
    assertTrue(attempts >= 3, "too few attempts to judge: " + attempts);
    LongTaskTimer connectTime = registry.find("pool.connect.time").longTaskTimer();
    // At most one attempt of the single connection can be in flight.
    assertTrue(
        connectTime.activeTasks() <= 1,
        "connect timer is not stopped on failure: "
            + connectTime.activeTasks()
            + " active for "
            + attempts
            + " attempts");
  }

  private IProtoClientPoolImpl createPool(MeterRegistry registry, PoolEventListener listener) {
    return createPool(registry, listener, server);
  }

  private IProtoClientPoolImpl createPool(
      MeterRegistry registry, PoolEventListener listener, ServerSocket target) {
    Bootstrap bootstrap =
        new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
    IProtoClientPoolImpl pool =
        new IProtoClientPoolImpl(
            new ConnectionFactory(bootstrap, timer),
            ManagedResource.external(timer),
            true,
            null,
            null,
            registry,
            null,
            null,
            false,
            listener);
    pool.setConnectTimeout(CONNECT_TIMEOUT_MS);
    pool.setReconnectAfter(RECONNECT_AFTER_MS);
    pool.setGroups(
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost(target.getInetAddress().getHostAddress())
                .withPort(target.getLocalPort())
                .withSize(1)
                .withTag(TAG)
                .build()));
    return pool;
  }

  private static MeterRegistry createMetricsRegistry() {
    MeterRegistry registry = new SimpleMeterRegistry();
    LongTaskTimer.builder("request.timer").register(registry);
    Counter.builder("request.counter").register(registry);
    Counter.builder("response.success").register(registry);
    Counter.builder("response.errors").register(registry);
    Counter.builder("response.ignored").register(registry);
    return registry;
  }
}

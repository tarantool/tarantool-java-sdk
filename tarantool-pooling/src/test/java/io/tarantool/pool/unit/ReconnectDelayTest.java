/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.unit;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

/** Reconnect delays of a pool entry whose server refuses connections. */
public class ReconnectDelayTest {

  private static final String TAG = "refused";

  private EventLoopGroup group;
  private Timer timer;
  private int refusedPort;
  private final List<Long> delays = Collections.synchronizedList(new ArrayList<>());
  private IProtoClientPoolImpl pool;

  @BeforeEach
  public void setUp() throws Exception {
    group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    timer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS);
    try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      refusedPort = socket.getLocalPort();
    }
    Bootstrap bootstrap =
        new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
    PoolEventListener listener =
        new PoolEventListener() {
          @Override
          public void onReconnectScheduled(String tag, int index, long delayMs) {
            delays.add(delayMs);
          }
        };
    pool =
        new IProtoClientPoolImpl(
            new ConnectionFactory(bootstrap, timer),
            ManagedResource.external(timer),
            true,
            null,
            null,
            null,
            null,
            null,
            false,
            listener);
  }

  @AfterEach
  public void tearDown() throws Exception {
    pool.close();
    timer.stop();
    group.shutdownGracefully().get();
  }

  @Test
  public void test_reconnectAfter_shouldApplyToExistingConnections() throws Exception {
    // TarantoolClientImpl sets the groups first and the reconnect delay after that.
    setGroups();
    pool.setReconnectAfter(120);
    pool.get(TAG, 0);

    awaitDelays(1);

    assertEquals(120L, delays.get(0));
  }

  private void setGroups() {
    pool.setGroups(
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost(InetAddress.getLoopbackAddress().getHostAddress())
                .withPort(refusedPort)
                .withSize(1)
                .withTag(TAG)
                .build()));
  }

  private void awaitDelays(int count) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (delays.size() < count && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
    assertTrue(delays.size() >= count, "reconnects scheduled: " + delays);
  }
}

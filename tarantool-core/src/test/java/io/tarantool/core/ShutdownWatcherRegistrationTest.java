/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import io.tarantool.core.connection.ConnectionFactory;

/** Reconnect attempts must not add a new {@code box.shutdown} callback each time. */
public class ShutdownWatcherRegistrationTest {

  private static final int ATTEMPTS = 5;

  private EventLoopGroup group;
  private Timer timer;
  private IProtoClientImpl client;

  @BeforeEach
  public void setUp() {
    group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    timer = new HashedWheelTimer();
    Bootstrap bootstrap =
        new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
    client = new IProtoClientImpl(new ConnectionFactory(bootstrap, timer), timer);
  }

  @AfterEach
  public void tearDown() throws Exception {
    client.close();
    timer.stop();
    group.shutdownGracefully().get();
  }

  @Test
  public void test_failedConnects_shouldRegisterShutdownCallbackOnce() throws Exception {
    InetSocketAddress refused = closedPortAddress();

    for (int i = 0; i < ATTEMPTS; i++) {
      CompletableFuture<Void> attempt = client.connect(refused, 1000, true);
      assertThrows(ExecutionException.class, () -> attempt.get(5, TimeUnit.SECONDS));
    }

    assertEquals(1, shutdownCallbacksCount());
  }

  private static InetSocketAddress closedPortAddress() throws Exception {
    try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      return new InetSocketAddress(socket.getInetAddress(), socket.getLocalPort());
    }
  }

  // The callbacks are not exposed by the API, so the test reads them directly.
  @SuppressWarnings("unchecked")
  private int shutdownCallbacksCount() throws Exception {
    Field watchersField = IProtoClientImpl.class.getDeclaredField("watchers");
    watchersField.setAccessible(true);
    Map<String, Watcher> watchers = (Map<String, Watcher>) watchersField.get(client);
    Field callbacksField = Watcher.class.getDeclaredField("callbacks");
    callbacksField.setAccessible(true);
    return ((List<?>) callbacksField.get(watchers.get("box.shutdown"))).size();
  }
}

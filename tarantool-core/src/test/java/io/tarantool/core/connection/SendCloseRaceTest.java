/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import io.tarantool.core.connection.exceptions.ConnectionClosedException;
import io.tarantool.core.protocol.requests.IProtoPing;

/**
 * A request passes the READY check, then the client closes the connection before netty writes it.
 */
public class SendCloseRaceTest {

  private static final long WAIT_SECONDS = 5;

  private EventLoopGroup group;
  private Timer timer;
  private ServerSocket server;
  private ConnectionImpl connection;

  @BeforeEach
  public void setUp() throws Exception {
    // One event loop: a task submitted to the group runs on the connection's channel thread.
    group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    timer = new HashedWheelTimer();
    server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
    Thread acceptor = new Thread(this::serveGreeting, "fake-tarantool");
    acceptor.setDaemon(true);
    acceptor.start();
    Bootstrap bootstrap =
        new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
    connection = new ConnectionImpl(bootstrap, timer);
  }

  @AfterEach
  public void tearDown() throws Exception {
    connection.close();
    server.close();
    timer.stop();
    group.shutdownGracefully().get();
  }

  @Test
  public void test_sendRacingClientClose_shouldFailWithConnectionClosed() throws Exception {
    InetSocketAddress address =
        new InetSocketAddress(server.getInetAddress(), server.getLocalPort());
    connection.connect(address, 3000).get(WAIT_SECONDS, TimeUnit.SECONDS);

    // Hold the event loop, so the write below is queued behind the close.
    CountDownLatch loopHeld = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    group
        .next()
        .execute(
            () -> {
              loopHeld.countDown();
              awaitQuietly(release);
              connection.close();
            });
    assertTrue(loopHeld.await(WAIT_SECONDS, TimeUnit.SECONDS));

    CompletableFuture<Void> sent = connection.send(new IProtoPing());
    release.countDown();

    ExecutionException ex =
        assertThrows(ExecutionException.class, () -> sent.get(WAIT_SECONDS, TimeUnit.SECONDS));
    Throwable root = ex.getCause();
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    assertInstanceOf(ConnectionClosedException.class, root);
    assertEquals("Connection closed by client", root.getMessage());
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(WAIT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Sends a Tarantool greeting and discards everything the client writes. */
  private void serveGreeting() {
    try (Socket socket = server.accept()) {
      OutputStream out = socket.getOutputStream();
      out.write(greeting());
      out.flush();
      InputStream in = socket.getInputStream();
      byte[] buf = new byte[1024];
      while (in.read(buf) >= 0) {
        // discard
      }
    } catch (Exception e) {
      // the server socket is closed in tearDown
    }
  }

  private static byte[] greeting() {
    String salt = Base64.getEncoder().encodeToString(new byte[32]);
    String text = line("Tarantool 3.5.0 (Binary) " + UUID.randomUUID()) + line(salt);
    return text.getBytes(StandardCharsets.US_ASCII);
  }

  private static String line(String s) {
    StringBuilder sb = new StringBuilder(s);
    while (sb.length() < 63) {
      sb.append(' ');
    }
    return sb.append('\n').toString();
  }
}

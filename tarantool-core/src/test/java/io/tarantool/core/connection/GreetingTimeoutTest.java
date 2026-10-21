/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** A server accepts the TCP connection but never sends the greeting. */
public class GreetingTimeoutTest {

  private static final long CONNECT_TIMEOUT_MS = 300;
  private static final long EVENT_WAIT_MS = 1000;

  private EventLoopGroup group;
  private Timer timer;
  private ServerSocket silentServer;
  private ConnectionImpl connection;

  @BeforeEach
  public void setUp() throws Exception {
    group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    timer = new HashedWheelTimer();
    // The kernel completes the TCP handshake from the backlog; nobody reads or writes.
    silentServer = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
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
    silentServer.close();
    timer.stop();
    group.shutdownGracefully().get();
  }

  @Test
  public void test_greetingTimeout_shouldNotBeReportedAsCloseByServer() throws Exception {
    List<Throwable> closedByRemote = new CopyOnWriteArrayList<>();
    connection.onClose(ConnectionCloseEvent.CLOSE_BY_REMOTE, (c, ex) -> closedByRemote.add(ex));

    InetSocketAddress address =
        new InetSocketAddress(silentServer.getInetAddress(), silentServer.getLocalPort());
    CompletableFuture<?> connect = connection.connect(address, CONNECT_TIMEOUT_MS);

    ExecutionException ex =
        assertThrows(ExecutionException.class, () -> connect.get(5, TimeUnit.SECONDS));
    assertInstanceOf(TimeoutException.class, ex.getCause());
    assertEquals(
        String.format(
            "Connection timeout: no greeting from %s within %d ms", address, CONNECT_TIMEOUT_MS),
        ex.getCause().getMessage());

    // Without the fix the close event follows the timeout within milliseconds.
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(EVENT_WAIT_MS);
    while (System.nanoTime() < deadline && closedByRemote.isEmpty()) {
      Thread.sleep(10);
    }
    assertEquals(
        0,
        closedByRemote.size(),
        "greeting timeout is reported as closed by server: " + closedByRemote);
    assertFalse(connection.isConnected());
  }

  @Test
  public void test_greetingTimerOfClosedAttempt_shouldNotCloseNextAttempt() throws Exception {
    long timeoutMs = 600;
    long pauseMs = 300;
    try (ServerSocket closingServer = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
      // Accepts and closes at once, before the greeting.
      Thread closer =
          new Thread(
              () -> {
                try (Socket socket = closingServer.accept()) {
                  // closed by try-with-resources
                } catch (Exception ignored) {
                  // the test fails on its own assertions
                }
              });
      closer.setDaemon(true);
      closer.start();

      CompletableFuture<?> first =
          connection.connect(
              new InetSocketAddress(closingServer.getInetAddress(), closingServer.getLocalPort()),
              timeoutMs);
      assertThrows(ExecutionException.class, () -> first.get(5, TimeUnit.SECONDS));
    }
    Thread.sleep(pauseMs);

    // The silent server records when the client closed its side of the second attempt.
    AtomicLong closedAfterMs = new AtomicLong(-1);
    long start = System.nanoTime();
    Thread reader =
        new Thread(
            () -> {
              try (Socket socket = silentServer.accept()) {
                while (socket.getInputStream().read() != -1) {
                  // the client never writes
                }
              } catch (Exception ignored) {
                // a reset also ends the connection
              }
              closedAfterMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            });
    reader.setDaemon(true);
    reader.start();

    CompletableFuture<?> second =
        connection.connect(
            new InetSocketAddress(silentServer.getInetAddress(), silentServer.getLocalPort()),
            timeoutMs);
    ExecutionException ex =
        assertThrows(ExecutionException.class, () -> second.get(5, TimeUnit.SECONDS));
    assertInstanceOf(TimeoutException.class, ex.getCause());
    reader.join(5000);

    // Without the fix the timer of the first attempt closes the second one about timeoutMs -
    // pauseMs after it starts, without any error for it.
    assertTrue(
        closedAfterMs.get() >= timeoutMs - 100,
        "second attempt was closed after " + closedAfterMs.get() + " ms");
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.integration;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.msgpack.value.ValueFactory.newArray;
import static org.msgpack.value.ValueFactory.newInteger;
import static org.msgpack.value.ValueFactory.newMap;
import io.netty.channel.ConnectTimeoutException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.tarantool.core.HelpersUtils.findRootCause;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_DATA;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_INDEX_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_KEY;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_LIMIT;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_OK;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_REQUEST_TYPE;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_SELECT;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_SPACE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_SYNC_ID;
import io.tarantool.core.connection.Connection;
import io.tarantool.core.connection.Greeting;
import io.tarantool.core.connection.exceptions.BadGreetingException;
import io.tarantool.core.connection.exceptions.ConnectionClosedException;
import io.tarantool.core.connection.exceptions.ConnectionException;
import io.tarantool.core.protocol.IProtoMessage;
import io.tarantool.core.protocol.IProtoRequest;
import io.tarantool.core.protocol.IProtoRequestImpl;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.protocol.requests.IProtoAuth;
import io.tarantool.core.protocol.requests.IProtoAuth.AuthType;

@Timeout(value = 5)
@Testcontainers
public class ConnectionToTarantoolTest extends BaseTest {

  private static final Logger log = LoggerFactory.getLogger(ConnectionToTarantoolTest.class);
  private static final String BAD_HOST = "128.0.0.1";
  private static final int BAD_PORT = 65535;
  private static final int CONCURRENT_THREADS_COUNT = 5;

  public static class MessageConsumer implements Consumer<IProtoResponse> {

    public final LinkedBlockingQueue<IProtoResponse> queue;

    public MessageConsumer() {
      this.queue = new LinkedBlockingQueue<>();
    }

    @Override
    public void accept(IProtoResponse msg) {
      try {
        this.queue.put(msg);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private static String version;
  private static String protocolType;
  private static UUID instanceUUID;
  private static int spaceId;

  @Container
  private static final TarantoolContainer tt =
      new TarantoolContainer()
          .withEnv(ENV_MAP)
          .withExposedPort(3302)
          .withExposedPort(3303)
          .withExposedPort(3304)
          .withExposedPort(3306);

  @BeforeAll
  public static void setUp() throws Exception {
    List<?> result = tt.executeCommandDecoded("return get_version()");
    version = (String) result.get(0);

    protocolType = "binary";

    result = tt.executeCommandDecoded("return box.info.uuid");
    String uuid = (String) result.get(0);
    instanceUUID = UUID.fromString(uuid);

    result = tt.executeCommandDecoded("return box.space.test.id");
    spaceId = (Integer) result.get(0);

    tt.executeCommandDecoded("lock_pipe(true)"); // for tests using 3305 port (no greeting)
  }

  @Test
  public void testConnect() throws Exception {
    Connection client = factory.create();
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), tt.getPort());
    CompletableFuture<Greeting> connectFuture =
        client.connect(
            address, 3_000); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    Greeting greeting = connectFuture.get();
    assertEquals(greeting.getVersion(), version);
    assertEquals(greeting.getProtocolType(), protocolType);
    assertEquals(greeting.getInstanceUUID(), instanceUUID);
  }

  /*
   * Connect to resolvable host and bad port. In this case ConnectionException is
   * being thrown, and its root cause should be ConnectException with reason,
   * explaining, that tcp connect cannot be established
   */
  @Test
  public void testConnectToAddressWithBadPort() {
    Connection client = factory.create();
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), BAD_PORT);
    CompletableFuture<Greeting> future =
        client.connect(
            address, 3_000); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    Exception ex = assertThrows(CompletionException.class, () -> future.join());
    Throwable cause = ex.getCause();
    assertEquals(ConnectionException.class, cause.getClass());
    assertEquals(
        String.format("Failed to connect to the Tarantool server at %s", address),
        cause.getMessage());
    assertEquals(ConnectException.class, findRootCause(ex).getClass());
  }

  /*
   * Connect to non-resolvable host and existing port. In this case
   * ConnectionException is being thrown, and its root cause should be
   * ConnectTimeoutException.  We check only (BAD_HOST, GOOD_PORT) case
   * because all port numbers are valid in range 0..65535.
   */
  @RepeatedTest(value = 3)
  public void testConnectToAddressWithBadHost() {
    Connection client = factory.create();
    InetSocketAddress address = new InetSocketAddress(BAD_HOST, tt.getPort());
    CompletableFuture<Greeting> future =
        client.connect(
            address, 5_000); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    Exception ex = assertThrows(CompletionException.class, () -> future.join());
    Throwable cause = ex.getCause();
    if (cause.getClass() == ConnectionException.class) {
      assertEquals(
          String.format("Failed to connect to the Tarantool server at %s", address),
          cause.getMessage());
      assertEquals(ConnectTimeoutException.class, findRootCause(ex).getClass());
    } else if (cause.getClass() == TimeoutException.class) {
      assertEquals("Connection timeout", cause.getMessage());
      log.info(findRootCause(ex).getClass().toString()); // Need for debug in the future
    } else {
      fail("An unknown exception was thrown during connection to the bad hostname!", cause);
    }
  }

  /*
   * Client tries to connect to server which does not call accept() for
   * incoming sockets, but closes suddenly bound socket. Should throw
   * SocketException with "Connection reset" message
   * */
  @Test
  public void testConnectToNonAcceptingService() {
    Integer otherPort =
        tt.getMappedPort(3302); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    Connection client = factory.create();
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), otherPort);
    CompletableFuture<Greeting> future =
        client.connect(
            address, 3_000); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    Exception ex = assertThrows(CompletionException.class, () -> future.join());
    Throwable cause = findRootCause(ex);

    /*
     * When we run container with disabled userland proxy we get
     * SocketException. When userland proxy is enabled, we normally connect
     * and then get closing immediately and ConnectionException is raised.
     * */
    assertTrue(
        Arrays.asList(
                ConnectionException.class,
                ConnectionClosedException.class,
                IOException.class,
                SocketException.class)
            .contains(cause.getClass()));
    String message = cause.getMessage();
    assertTrue(
        Arrays.asList(
                "Connection closed by server",
                "Connection reset",
                "Connection reset by peer",
                "Соединение разорвано другой стороной")
            .contains(message),
        message);
  }

  @Test
  public void testConnectToServiceWithBadGreeting() {
    Integer otherPort = tt.getMappedPort(3304);
    Connection client = factory.create();
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), otherPort);
    CompletableFuture<Greeting> future =
        client.connect(
            address, 3_000); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    Exception ex = assertThrows(CompletionException.class, () -> future.join());
    Throwable cause = findRootCause(ex);
    assertEquals(BadGreetingException.class, cause.getClass());
    assertTrue(cause.getMessage().startsWith("bad greeting start:"));
  }

  /*
   * Client tries to connect to server which does not call accept() for
   * incoming sockets for a long time. Should throw
   * ConnectionException with "Connection timeout" message
   * */
  @Test
  public void testConnectToSilentNode() {
    Integer otherPort = tt.getMappedPort(3303);
    Connection client = factory.create().listen(msg -> {});
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), otherPort);
    CompletableFuture<Greeting> future = client.connect(address, 3000);
    Exception ex = assertThrows(CompletionException.class, () -> future.join());
    Throwable cause = findRootCause(ex);
    assertEquals(TimeoutException.class, cause.getClass());
    assertEquals("Connection timeout", cause.getMessage());
  }

  /*
   * Client tries to connect to server which does not call accept() for
   * incoming sockets for a long time. Should throw
   * ConnectionException with "Connection timeout" message
   * */
  @Test
  public void testConnectToSilentNodeAndClose() throws Exception {
    Integer otherPort = tt.getMappedPort(3303);
    Connection client = factory.create().listen(msg -> {});
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), otherPort);
    CompletableFuture<Greeting> future = client.connect(address, 3000);
    Thread.sleep(100);
    client.close();
    Exception ex = assertThrows(CompletionException.class, () -> future.join());
    Throwable cause = findRootCause(ex);
    assertEquals(ConnectionClosedException.class, cause.getClass());
    assertEquals("Connection closed by client", cause.getMessage());
  }

  /*
   * Client tries to connect and some code tries to close this connection
   * (for example, in multithreaded environment) due to some reason. So, this
   * part of code which requests connection should retrieve an exception with
   * explanation, why it was closed.
   */
  @Test
  public void testConnectWithClosingOnClientSide() throws Exception {
    Connection client = factory.create();
    InetSocketAddress address = new InetSocketAddress(BAD_HOST, BAD_PORT);
    CompletableFuture<Greeting> future =
        client.connect(
            address, 3_000); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    Thread.sleep(100);
    client.close();
    Exception ex = assertThrows(CompletionException.class, () -> future.join());
    Throwable cause = ex.getCause();
    assertEquals(cause.getClass(), ConnectionException.class);
    assertEquals("Connection closed by client", cause.getMessage());
    assertEquals(ClosedChannelException.class, findRootCause(ex).getClass());
  }

  /**
   * Check for concurrent connect by multiple threads. All threads connecting concurrently should
   * get the same future. Only one thread will start connection process and wait for future
   * completion, remaining threads only wait for future.
   *
   * @throws InterruptedException
   */
  @Test
  public void testConnectByMultipleThreads() throws InterruptedException {
    Connection client = factory.create();
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), tt.getPort());
    LinkedBlockingQueue<CompletableFuture<Greeting>> promises = new LinkedBlockingQueue<>();
    for (int i = 0; i < CONCURRENT_THREADS_COUNT; i++) {
      new Thread(() -> promises.add(client.connect(address, 3_000)))
          .start(); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    }
    int failed = 0;
    int success = 0;
    for (int i = 0; i < CONCURRENT_THREADS_COUNT; i++) {
      CompletableFuture<?> p = promises.take();
      try {
        p.get();
        success++;
      } catch (ExecutionException ex) {
        Throwable cause = ex.getCause();
        assertEquals(ConnectionException.class, cause.getClass());
        failed++;
      }
    }
    assertEquals(0, failed);
    assertEquals(CONCURRENT_THREADS_COUNT, success);
  }

  @Test
  public void testConnectWithGreetingTimeout() throws InterruptedException {
    Connection client = factory.create();
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), tt.getMappedPort(3306));
    CompletableFuture<Greeting> future = client.connect(address, 1_000);
    Exception ex = assertThrows(CompletionException.class, () -> future.join());
    Throwable cause = ex.getCause();
    assertTrue(
        cause instanceof TimeoutException || cause instanceof ConnectionClosedException,
        "Exception should be either TimeoutException or ConnectionClosedException");
    assertEquals("Connection timeout", cause.getMessage());
  }

  @Test
  public void testConnectWithWaitingForGreetingAndClose() throws Exception {
    Connection client = factory.create();
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), tt.getMappedPort(3306));
    CompletableFuture<Greeting> future = client.connect(address, 2_000);
    Thread.sleep(500);
    client.close();
    Exception ex = assertThrows(CompletionException.class, () -> future.join());
    Throwable cause = ex.getCause();
    assertEquals(ConnectionClosedException.class, cause.getClass());
    assertEquals("Connection closed by client", cause.getMessage());
  }

  @Test
  public void testConnectAndSendByMultipleThreads() throws InterruptedException {
    Connection client = factory.create();
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), tt.getPort());
    LinkedBlockingQueue<CompletableFuture<Void>> promises = new LinkedBlockingQueue<>();
    IProtoRequest msg = createSelectRequest(0);
    for (int i = 0; i < CONCURRENT_THREADS_COUNT; i++) {
      new Thread(
              () -> {
                CompletableFuture<Void> future =
                    client
                        .connect(
                            address,
                            3_000) // todo https://github.com/tarantool/tarantool-java-ee/issues/412
                        .thenCompose(g -> client.send(msg));
                promises.add(future);
              })
          .start();
    }
    int failed = 0;
    int success = 0;
    for (int i = 0; i < CONCURRENT_THREADS_COUNT; i++) {
      CompletableFuture<?> p = promises.take();
      try {
        p.get();
        success++;
      } catch (ExecutionException ex) {
        Throwable cause = ex.getCause();
        assertEquals(ConnectionException.class, cause.getClass());
        failed++;
      }
    }
    assertEquals(0, failed);
    assertEquals(CONCURRENT_THREADS_COUNT, success);
  }

  private IProtoRequest createSelectRequest(int syncId) {
    Map<Value, Value> rawHeader = new HashMap<>();
    rawHeader.put(newInteger(IPROTO_REQUEST_TYPE), newInteger(IPROTO_SELECT));
    rawHeader.put(newInteger(IPROTO_SYNC_ID), newInteger(syncId));

    Map<Value, Value> rawBody = new HashMap<>();
    rawBody.put(newInteger(IPROTO_KEY), newArray(ValueFactory.newString("testkey")));
    rawBody.put(newInteger(IPROTO_SPACE_ID), newInteger(spaceId));
    rawBody.put(newInteger(IPROTO_INDEX_ID), newInteger(0));
    rawBody.put(newInteger(IPROTO_LIMIT), newInteger(10));

    return new IProtoRequestImpl(newMap(rawHeader), newMap(rawBody));
  }

  @Test
  public void testIProtoSendAndReceive() throws Exception {
    MessageConsumer consumer = new MessageConsumer();
    Connection client = factory.create().listen(consumer);
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), tt.getPort());
    client
        .connect(address, 3_000)
        .join(); // todo https://github.com/tarantool/tarantool-java-ee/issues/412

    Greeting greeting = client.getGreeting().get();
    IProtoRequest request =
        new IProtoAuth(API_USER, CREDS.get(API_USER), greeting.getSalt(), AuthType.CHAP_SHA1);
    request.setSyncId(0);

    client.send(request).join();

    IProtoRequest msg = createSelectRequest(0);
    assertNull(client.send(msg).join());
    Thread.sleep(100);
    assertEquals(consumer.queue.size(), 2);

    consumer.queue.take();
    IProtoMessage reply = consumer.queue.take();
    Map<Value, Value> header = reply.getHeader().asMapValue().map();
    assertEquals(newInteger(IPROTO_OK), header.get(newInteger(IPROTO_REQUEST_TYPE)));
    assertEquals(newInteger(0), header.get(newInteger(IPROTO_SYNC_ID)));

    Map<Value, Value> body = reply.getBody().asMapValue().map();
    // No data in space
    assertEquals(newArray(), body.get(newInteger(IPROTO_DATA)));
  }

  @Test
  public void testSendAndReceiveWithConcurrentClose() throws Exception {
    MessageConsumer consumer = new MessageConsumer();
    Connection client = factory.create().listen(consumer);
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), tt.getPort());
    client
        .connect(address, 3_000)
        .join(); // todo https://github.com/tarantool/tarantool-java-ee/issues/412

    List<CompletableFuture<Void>> futures = new ArrayList<>();
    CountDownLatch errorsCounter = new CountDownLatch(5);
    CountDownLatch totalRequests = new CountDownLatch(10);

    Thread sender =
        new Thread(
            () -> {
              int syncId = 0;
              while (errorsCounter.getCount() > 0) {
                IProtoMessage msg = createSelectRequest(syncId++);
                try {
                  CompletableFuture<Void> promise = client.send(msg);
                  futures.add(promise);
                  promise.whenComplete(
                      (r, e) -> {
                        if (e != null) {
                          errorsCounter.countDown();
                        }
                      });
                  totalRequests.countDown();
                } catch (IllegalStateException e) {
                  errorsCounter.countDown();
                }
              }
            });

    sender.start();
    totalRequests.await();
    client.close();
    sender.join();

    int connectionClosedByClient = 0;
    int otherExceptions = 0;
    int failedFutures = 0;
    int successFutures = 0;
    for (CompletableFuture<Void> future : futures) {
      try {
        future.join();
        successFutures++;
      } catch (Exception e) {
        failedFutures++;
        assertEquals(CompletionException.class, e.getClass());
        Throwable cause = findRootCause(e);
        assertEquals(ConnectionClosedException.class, cause.getClass());
        if (cause.getMessage().equals("Connection closed by client")) {
          connectionClosedByClient++;
        } else {
          otherExceptions++;
        }
      }
    }
    assertTrue(failedFutures >= 0);
    assertTrue(connectionClosedByClient >= 0);
    assertTrue(successFutures >= 10);
    assertEquals(0, otherExceptions);
    assertEquals(failedFutures, connectionClosedByClient);
  }
}

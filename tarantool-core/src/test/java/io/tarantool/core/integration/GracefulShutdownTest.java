/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.integration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.ValueFactory;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.utils.TarantoolContainerClientHelper;

import static io.tarantool.core.HelpersUtils.findRootCause;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.IProtoClientImpl;
import io.tarantool.core.connection.exceptions.ConnectionClosedException;
import io.tarantool.core.exceptions.ShutdownException;
import io.tarantool.core.protocol.IProtoResponse;

@Timeout(value = 5)
public class GracefulShutdownTest extends BaseTest {

  private static InetSocketAddress address;

  private static TarantoolContainer<?> tt;

  @BeforeAll
  public static void setUp() {
    tt = TarantoolContainerClientHelper.createTarantoolContainer().withEnv(ENV_MAP);
    tt.start();
    TarantoolContainerClientHelper.execInitScript(tt);

    address = tt.mappedAddress();
  }

  @AfterAll
  static void tearDown() {
    tt.stop();
  }

  private IProtoClient getClientAndConnect() throws Exception {
    IProtoClient client = new IProtoClientImpl(factory, factory.getTimerService());
    client.connect(address, 3_000).get();
    return client;
  }

  @Test
  public void testShutdown() throws Exception {
    IProtoClient client = getClientAndConnect();
    client.authorize(API_USER, CREDS.get(API_USER)).join();

    List<CompletableFuture<IProtoResponse>> futures = new ArrayList<>();

    ArrayValue args = ValueFactory.emptyArray();
    for (int i = 0; i < 1000; i++) {
      futures.add(client.call("slow_echo", args));
    }

    int killTTAfterFutures = 10;
    boolean killed = false;
    int failedFutures = 0;
    int successFutures = 0;

    int connectionClosedByShutdown = 0;
    int connectionNotEstablished = 0;
    int requestFinishedByShutdown = 0;

    int otherExceptions = 0;

    for (CompletableFuture<IProtoResponse> future : futures) {
      try {
        killTTAfterFutures--;
        future.get();
        successFutures++;
      } catch (Exception e) {
        failedFutures++;
        assertEquals(ExecutionException.class, e.getClass());
        Throwable cause = findRootCause(e);
        Class<? extends Throwable> causeClass = cause.getClass();
        String message = cause.getMessage();
        if (causeClass.equals(ShutdownException.class)) {
          assertEquals("Request finished by shutdown", message);
          requestFinishedByShutdown++;
        } else {
          assertEquals(ConnectionClosedException.class, causeClass);
          if (message.equals("Connection closed by shutdown")) {
            connectionClosedByShutdown++;
          } else if (message.equals("Connection is not established")) {
            connectionNotEstablished++;
          } else {
            otherExceptions++;
          }
        }
      }
      if (killTTAfterFutures <= 0 && !killed) {
        try {
          // send sigterm to tarantool
          tt.stop();
          killed = true;
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    }
    assertTrue(failedFutures > 0);
    assertTrue(successFutures > 0);
    assertTrue(connectionClosedByShutdown >= 0);
    assertTrue(connectionNotEstablished >= 0);
  }
}

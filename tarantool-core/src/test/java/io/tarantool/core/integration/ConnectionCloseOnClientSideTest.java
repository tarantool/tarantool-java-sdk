/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.integration;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.testcontainers.containers.utils.TarantoolContainerClientHelper.createTarantoolContainer;
import static org.testcontainers.containers.utils.TarantoolContainerClientHelper.execInitScript;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.tarantool.TarantoolContainer;

import static io.tarantool.core.HelpersUtils.findRootCause;
import io.tarantool.core.connection.Connection;
import io.tarantool.core.connection.ConnectionCloseEvent;
import io.tarantool.core.connection.exceptions.ConnectionClosedException;

@Timeout(value = 5)
public class ConnectionCloseOnClientSideTest extends BaseTest {

  private static TarantoolContainer<?> tt;

  @BeforeAll
  static void setUp() {
    tt = createTarantoolContainer().withEnv(ENV_MAP);
    tt.start();
    execInitScript(tt);
  }

  @AfterAll
  static void tearDown() {
    tt.stop();
  }

  @Test
  public void testConnectAndClose() throws Exception {
    Connection connection = factory.create();
    CompletableFuture<Boolean> closeFuture = new CompletableFuture<>();
    connection.onClose(
        ConnectionCloseEvent.CLOSE_BY_CLIENT,
        (c, ex) -> {
          closeFuture.completeExceptionally(ex);
        });
    InetSocketAddress address = tt.mappedAddress();
    connection.connect(address, 3_000).get();
    Thread.sleep(500);
    connection.close();
    Exception ex = assertThrows(CompletionException.class, closeFuture::join);
    Throwable cause = ex.getCause();
    assertEquals(ConnectionClosedException.class, cause.getClass());
    assertEquals(ConnectionClosedException.class, findRootCause(ex).getClass());
    assertEquals("Connection closed by client", cause.getMessage());
  }

  @Test
  public void testConnectAndCloseShutdown() throws Exception {
    Connection connection = factory.create();
    CompletableFuture<Boolean> closeFuture = new CompletableFuture<>();
    connection.onClose(
        ConnectionCloseEvent.CLOSE_BY_SHUTDOWN,
        (c, ex) -> {
          closeFuture.completeExceptionally(ex);
        });
    InetSocketAddress address = tt.mappedAddress();
    connection.connect(address, 3_000).get();
    Thread.sleep(500);
    connection.shutdownClose();
    Exception ex = assertThrows(CompletionException.class, closeFuture::join);
    Throwable cause = ex.getCause();
    assertEquals(ConnectionClosedException.class, cause.getClass());
    assertEquals(ConnectionClosedException.class, findRootCause(ex).getClass());
    assertEquals("Connection closed by shutdown", cause.getMessage());
  }
}

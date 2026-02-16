/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.integration;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.utils.TarantoolContainerClientHelper;

import static io.tarantool.core.HelpersUtils.findRootCause;
import io.tarantool.core.connection.Connection;
import io.tarantool.core.connection.ConnectionCloseEvent;
import io.tarantool.core.connection.exceptions.ConnectionClosedException;

@Timeout(value = 7)
public class ConnectionCloseOnServerSideTest extends BaseTest {

  private static TarantoolContainer<?> tt;

  @BeforeAll
  static void setUp() {
    tt = TarantoolContainerClientHelper.createTarantoolContainer().withEnv(ENV_MAP);
    tt.start();
    TarantoolContainerClientHelper.execInitScript(tt);
  }

  @AfterAll
  static void tearDown() {
    tt.stop();
  }

  @Test
  public void testConnectAndCloseOnServer() throws Exception {
    Map<ConnectionCloseEvent, Boolean> flags = new HashMap<>();
    flags.put(ConnectionCloseEvent.CLOSE_BY_REMOTE, false);
    flags.put(ConnectionCloseEvent.CLOSE_BY_CLIENT, false);
    flags.put(ConnectionCloseEvent.CLOSE_BY_SHUTDOWN, false);
    Connection connection = factory.create();
    CompletableFuture<Boolean> closeFuture = new CompletableFuture<>();
    connection.onClose(
        ConnectionCloseEvent.CLOSE_BY_REMOTE,
        (c, ex) -> {
          closeFuture.completeExceptionally(ex);
        });
    connection.onClose(
        ConnectionCloseEvent.CLOSE_BY_REMOTE,
        (c, ex) -> {
          flags.put(ConnectionCloseEvent.CLOSE_BY_REMOTE, true);
        });
    connection.onClose(
        ConnectionCloseEvent.CLOSE_BY_CLIENT,
        (c, ex) -> {
          flags.put(ConnectionCloseEvent.CLOSE_BY_CLIENT, true);
        });
    connection.onClose(
        ConnectionCloseEvent.CLOSE_BY_SHUTDOWN,
        (c, ex) -> {
          flags.put(ConnectionCloseEvent.CLOSE_BY_SHUTDOWN, true);
        });
    InetSocketAddress address = tt.mappedAddress();
    connection.connect(address, 3_000).get();
    tt.stop();
    Exception ex = assertThrows(CompletionException.class, closeFuture::join);
    Throwable cause = ex.getCause();
    assertEquals(ConnectionClosedException.class, cause.getClass());
    assertEquals(ConnectionClosedException.class, findRootCause(ex).getClass());
    assertEquals("Connection closed by server", cause.getMessage());
    assertTrue(flags.get(ConnectionCloseEvent.CLOSE_BY_REMOTE));
    assertFalse(flags.get(ConnectionCloseEvent.CLOSE_BY_CLIENT));
    assertFalse(flags.get(ConnectionCloseEvent.CLOSE_BY_SHUTDOWN));
  }
}

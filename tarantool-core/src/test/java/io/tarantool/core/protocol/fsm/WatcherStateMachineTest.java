/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.fsm;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERROR_BASE;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_OK;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_REQUEST_TYPE;
import io.tarantool.core.WatcherOptions;
import io.tarantool.core.connection.Connection;
import io.tarantool.core.connection.ConnectionCloseEvent;
import io.tarantool.core.connection.Greeting;
import io.tarantool.core.protocol.IProtoMessage;
import io.tarantool.core.protocol.IProtoRawResponse;
import io.tarantool.core.protocol.IProtoResponse;

@Timeout(5)
class WatcherStateMachineTest {

  private final Timer timer = new HashedWheelTimer();

  @AfterEach
  void tearDown() {
    timer.stop();
  }

  private WatcherStateMachine newWatcher(Connection connection) {
    return new WatcherStateMachine(
        "box.shutdown", 1L, response -> {}, connection, WatcherOptions.builder().build(), timer);
  }

  private static IProtoResponse response(boolean error) {
    Map<Value, Value> header = new HashMap<>();
    header.put(
        MP_IPROTO_REQUEST_TYPE, ValueFactory.newInteger(error ? IPROTO_ERROR_BASE : IPROTO_OK));
    return new IProtoRawResponse(ValueFactory.newMap(header), new byte[0], 0);
  }

  @Test
  void registeredCompletesOnFirstEvent() {
    WatcherStateMachine watcher = newWatcher(new FakeConnection());
    watcher.runOnce();
    assertFalse(watcher.registered().isDone());

    watcher.process(response(false));

    assertTrue(watcher.registered().isDone());
    assertFalse(watcher.registered().isCompletedExceptionally());
  }

  @Test
  void registeredFailsOnErrorEvent() {
    WatcherStateMachine watcher = newWatcher(new FakeConnection());
    watcher.runOnce();

    watcher.process(response(true));

    assertTrue(watcher.registered().isCompletedExceptionally());
  }

  @Test
  void registeredFailsOnKill() {
    WatcherStateMachine watcher = newWatcher(new FakeConnection());
    watcher.runOnce();

    watcher.kill(new RuntimeException("connection closed"));

    assertTrue(watcher.registered().isCompletedExceptionally());
  }

  @Test
  void registeredFailsWhenSendFails() {
    FakeConnection connection = new FakeConnection();
    CompletableFuture<Void> failed = new CompletableFuture<>();
    failed.completeExceptionally(new RuntimeException("send failed"));
    connection.sendResult = failed;
    WatcherStateMachine watcher = newWatcher(connection);

    watcher.runOnce();

    assertTrue(watcher.registered().isCompletedExceptionally());
  }

  @Test
  void registeredFailsWhenSendTimesOut() {
    FakeConnection connection = new FakeConnection();
    connection.sendResult = new CompletableFuture<>();
    WatcherStateMachine watcher =
        new WatcherStateMachine(
            "box.shutdown",
            1L,
            response -> {},
            connection,
            WatcherOptions.builder().withSendTimeout(50).build(),
            timer);

    watcher.runOnce();

    assertThrows(Exception.class, () -> watcher.registered().get(2, TimeUnit.SECONDS));
    assertTrue(watcher.registered().isCompletedExceptionally());
  }

  private static final class FakeConnection implements Connection {
    private CompletableFuture<Void> sendResult = CompletableFuture.completedFuture(null);

    @Override
    public CompletableFuture<Void> send(IProtoMessage message) {
      return sendResult;
    }

    @Override
    public boolean isConnected() {
      return true;
    }

    @Override
    public CompletableFuture<Greeting> connect(InetSocketAddress addr, long timeoutMs) {
      return new CompletableFuture<>();
    }

    @Override
    public Connection listen(Consumer<IProtoResponse> listener) {
      return this;
    }

    @Override
    public Connection onClose(
        ConnectionCloseEvent event, BiConsumer<Connection, Throwable> handler) {
      return this;
    }

    @Override
    public Optional<Greeting> getGreeting() {
      return Optional.empty();
    }

    @Override
    public void shutdownClose() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void setIdleTimeout(int idleTimeout) {}

    @Override
    public boolean isPaused() {
      return false;
    }

    @Override
    public void close() {}
  }
}

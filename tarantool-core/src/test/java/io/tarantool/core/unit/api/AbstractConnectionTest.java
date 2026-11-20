/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.unit.api;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.tarantool.core.api.connection.AbstractConnection;
import io.tarantool.core.api.connection.Connection;
import io.tarantool.core.api.connection.reason.CloseReason;
import io.tarantool.core.connection.Greeting;
import io.tarantool.core.protocol.IProtoMessage;
import io.tarantool.core.protocol.IProtoResponse;

class AbstractConnectionTest {

  private static final Duration VALID_CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration VALID_CLOSE_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration VALID_IDLE_TIMEOUT = Duration.ofSeconds(2);
  private static final InetSocketAddress VALID_ADDRESS =
      InetSocketAddress.createUnresolved("localhost", 3301);
  private static final BiConsumer<Connection, CloseReason> VALID_CLOSE_HANDLER = (c, r) -> {};
  private static final Consumer<IProtoResponse> VALID_RESPONSE_HANDLER = r -> {};

  @Test
  void testValid() throws Exception {
    try (Connection connection =
        createConnection(
            VALID_CONNECT_TIMEOUT,
            VALID_CLOSE_TIMEOUT,
            VALID_IDLE_TIMEOUT,
            VALID_ADDRESS,
            VALID_CLOSE_HANDLER,
            VALID_RESPONSE_HANDLER)) {

      Assertions.assertEquals(VALID_CONNECT_TIMEOUT, connection.connectTimeout());
      Assertions.assertEquals(VALID_CLOSE_TIMEOUT, connection.closeTimeout());
      Assertions.assertEquals(VALID_ADDRESS, connection.address());
      Assertions.assertSame(VALID_CLOSE_HANDLER, connection.onCloseHandler());
      Assertions.assertSame(VALID_RESPONSE_HANDLER, connection.responseHandler());
    }
  }

  static Stream<Arguments> invalidConstructorArguments() {
    return Stream.of(
        Arguments.of(
            null,
            VALID_CLOSE_TIMEOUT,
            VALID_IDLE_TIMEOUT,
            VALID_ADDRESS,
            VALID_CLOSE_HANDLER,
            VALID_RESPONSE_HANDLER,
            NullPointerException.class,
            "connectTimeout must not be null!"),
        Arguments.of(
            VALID_CONNECT_TIMEOUT,
            null,
            VALID_IDLE_TIMEOUT,
            VALID_ADDRESS,
            VALID_CLOSE_HANDLER,
            VALID_RESPONSE_HANDLER,
            NullPointerException.class,
            "closeTimeout must not be null!"),
        Arguments.of(
            VALID_CONNECT_TIMEOUT,
            VALID_CLOSE_TIMEOUT,
            VALID_IDLE_TIMEOUT,
            null,
            VALID_CLOSE_HANDLER,
            VALID_RESPONSE_HANDLER,
            NullPointerException.class,
            "address must not be null!"),
        Arguments.of(
            VALID_CONNECT_TIMEOUT,
            VALID_CLOSE_TIMEOUT,
            VALID_IDLE_TIMEOUT,
            VALID_ADDRESS,
            null,
            VALID_RESPONSE_HANDLER,
            NullPointerException.class,
            "onCloseHandler must not be null!"),
        Arguments.of(
            VALID_CONNECT_TIMEOUT,
            VALID_CLOSE_TIMEOUT,
            VALID_IDLE_TIMEOUT,
            VALID_ADDRESS,
            VALID_CLOSE_HANDLER,
            null,
            NullPointerException.class,
            "responseHandler must not be null!"),
        Arguments.of(
            Duration.ZERO,
            VALID_CLOSE_TIMEOUT,
            VALID_IDLE_TIMEOUT,
            VALID_ADDRESS,
            VALID_CLOSE_HANDLER,
            VALID_RESPONSE_HANDLER,
            IllegalArgumentException.class,
            "connectTimeout must be > 0!"),
        Arguments.of(
            Duration.ofNanos(-1),
            VALID_CLOSE_TIMEOUT,
            VALID_IDLE_TIMEOUT,
            VALID_ADDRESS,
            VALID_CLOSE_HANDLER,
            VALID_RESPONSE_HANDLER,
            IllegalArgumentException.class,
            "connectTimeout must be > 0!"),
        Arguments.of(
            Duration.ofSeconds(-10),
            VALID_CLOSE_TIMEOUT,
            VALID_IDLE_TIMEOUT,
            VALID_ADDRESS,
            VALID_CLOSE_HANDLER,
            VALID_RESPONSE_HANDLER,
            IllegalArgumentException.class,
            "connectTimeout must be > 0!"),
        Arguments.of(
            VALID_CONNECT_TIMEOUT,
            Duration.ZERO,
            VALID_IDLE_TIMEOUT,
            VALID_ADDRESS,
            VALID_CLOSE_HANDLER,
            VALID_RESPONSE_HANDLER,
            IllegalArgumentException.class,
            "closeTimeout must be > 0!"),
        Arguments.of(
            VALID_CONNECT_TIMEOUT,
            Duration.ofNanos(-1),
            VALID_IDLE_TIMEOUT,
            VALID_ADDRESS,
            VALID_CLOSE_HANDLER,
            VALID_RESPONSE_HANDLER,
            IllegalArgumentException.class,
            "closeTimeout must be > 0!"),
        Arguments.of(
            VALID_CONNECT_TIMEOUT,
            Duration.ofSeconds(-10),
            VALID_IDLE_TIMEOUT,
            VALID_ADDRESS,
            VALID_CLOSE_HANDLER,
            VALID_RESPONSE_HANDLER,
            IllegalArgumentException.class,
            "closeTimeout must be > 0!"));
  }

  @ParameterizedTest(name = "{index} => {6}")
  @MethodSource("invalidConstructorArguments")
  void testInvalidParameters(
      Duration connectTimeout,
      Duration closeTimeout,
      Duration idleTimeout,
      InetSocketAddress address,
      BiConsumer<Connection, CloseReason> onCloseHandler,
      Consumer<IProtoResponse> responseHandler,
      Class<? extends Throwable> expectedException,
      String expectedMessage) {
    Throwable ex =
        Assertions.assertThrows(
            expectedException,
            () ->
                createConnection(
                    connectTimeout,
                    closeTimeout,
                    idleTimeout,
                    address,
                    onCloseHandler,
                    responseHandler));
    Assertions.assertEquals(expectedMessage, ex.getMessage());
  }

  private static AbstractConnection createConnection(
      Duration connectTimeout,
      Duration closeTimeout,
      Duration idleTimeout,
      InetSocketAddress address,
      BiConsumer<Connection, CloseReason> onCloseHandler,
      Consumer<IProtoResponse> responseHandler) {

    return new AbstractConnection(
        connectTimeout, closeTimeout, idleTimeout, address, onCloseHandler, responseHandler) {
      @Override
      public CompletionStage<Greeting> connect() {
        return CompletableFuture.completedFuture(null);
      }

      @Override
      public CompletionStage<Void> send(IProtoMessage message) {
        return CompletableFuture.completedFuture(null);
      }

      @Override
      public boolean isConnected() {
        return false;
      }

      @Override
      public CompletionStage<Void> closeAsync() {
        return CompletableFuture.completedFuture(null);
      }

      @Override
      public void close() {}

      @Override
      public CompletionStage<Void> shutdownCloseAsync() {
        return CompletableFuture.completedFuture(null);
      }

      @Override
      public void shutdownClose() {}
    };
  }
}

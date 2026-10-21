/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.connection.exceptions.ConnectionClosedException;
import io.tarantool.core.connection.exceptions.ConnectionException;
import io.tarantool.core.connection.exceptions.IdleTimeoutException;
import io.tarantool.core.protocol.IProtoMessage;
import io.tarantool.core.protocol.IProtoResponse;

/** The type Connection. */
public class ConnectionImpl implements Connection {

  private enum CloseReason {
    /** No reason close reason. */
    NO_REASON(ERR_CONNECT_FAILED),
    /** Closed by client close reason. */
    CLOSED_BY_CLIENT(ERR_CLOSED_BY_CLIENT),
    /** Closed by shutdown close reason. */
    CLOSED_BY_SHUTDOWN(ERR_CLOSED_BY_SHUTDOWN),
    /** Closed by the client because the greeting did not arrive in time. */
    CLOSED_BY_TIMEOUT(ERR_CLOSED_BY_TIMEOUT);

    private final String message;

    CloseReason(String message) {
      this.message = message;
    }

    /**
     * Gets message.
     *
     * @return the message
     */
    public String getMessage() {
      return message;
    }
  }

  private enum State {
    /** Closed state. */
    CLOSED(ERR_NOT_CONNECTED),
    /** Connecting state. */
    CONNECTING(ERR_CONNECTING),
    /** Waiting state. */
    WAITING(ERR_WAITING),
    /** Ready state. */
    READY(ERR_CONNECTED),
    CLOSING(ERR_CLOSING);

    private final String message;

    State(String message) {
      this.message = message;
    }

    /**
     * Gets message.
     *
     * @return the message
     */
    public String getMessage() {
      return message;
    }
  }

  private static final String ERR_CONNECT_FAILED =
      "Failed to connect to the Tarantool server at %s";
  private static final String ERR_CONNECTED = "Connection is established";
  private static final String ERR_CONNECTING = "Connection is establishing";
  private static final String ERR_CLOSING = "Connection is closing";
  private static final String ERR_WAITING = "Connection is waiting for greeting";
  private static final String ERR_CLOSED_BY_CLIENT = "Connection closed by client";
  private static final String ERR_CLOSED_BY_SERVER = "Connection closed by server";
  private static final String ERR_CLOSED_BY_SHUTDOWN = "Connection closed by shutdown";
  private static final String ERR_CLOSED_BY_TIMEOUT = "Connection closed by connect timeout";
  private static final String ERR_GREETING_TIMEOUT =
      "Connection timeout: no greeting from %s within %d ms";
  private static final String ERR_NOT_CONNECTED = "Connection is not established";
  private static final String ERR_SEND_FAILURE = "Failed to send IProto message: %s";

  /** The constant log. */
  static final Logger log = LoggerFactory.getLogger(ConnectionImpl.class);

  private final Bootstrap bootstrap;
  private final Timer timerService;
  private final AtomicReference<State> state;
  private final AtomicReference<CloseReason> closedBy;
  private final Map<ConnectionCloseEvent, List<BiConsumer<Connection, Throwable>>> closeListeners;
  private final FlushConsolidationHandler flushConsolidationHandler;
  private final BiConsumer<Connection, Throwable> idleTimeoutConsumer;

  private Channel channel;
  private Greeting greeting;
  private final SslContext sslContext;
  private Consumer<IProtoResponse> consumer;
  private CompletableFuture<Greeting> connectPromise;
  private Timeout timeoutHandler;
  private int idleTimeout;

  /**
   * Instantiates a new Connection.
   *
   * @param bootstrap the bootstrap
   * @param timerService the timer service
   */
  protected ConnectionImpl(Bootstrap bootstrap, Timer timerService) {
    this(bootstrap, null, timerService, null);
  }

  /**
   * Instantiates a new Connection.
   *
   * @param bootstrap the bootstrap
   * @param sslContext the ssl context
   * @param timerService the timer service
   * @param flushConsolidationHandler the flush consolidation handler
   */
  protected ConnectionImpl(
      Bootstrap bootstrap,
      SslContext sslContext,
      Timer timerService,
      FlushConsolidationHandler flushConsolidationHandler) {
    this(bootstrap, sslContext, timerService, flushConsolidationHandler, (c, err) -> {});
  }

  /**
   * Instantiates a new Connection.
   *
   * @param bootstrap the bootstrap
   * @param sslContext the ssl context
   * @param timerService the timer service
   * @param flushConsolidationHandler the flush consolidation handler
   * @param idleTimeoutConsumer the consumer for errors of idle timeout
   */
  protected ConnectionImpl(
      Bootstrap bootstrap,
      SslContext sslContext,
      Timer timerService,
      FlushConsolidationHandler flushConsolidationHandler,
      BiConsumer<Connection, Throwable> idleTimeoutConsumer) {
    this.bootstrap = bootstrap;
    this.state = new AtomicReference<>(State.CLOSED);
    this.closedBy = new AtomicReference<>(CloseReason.NO_REASON);
    this.closeListeners = new ConcurrentHashMap<>();
    this.sslContext = sslContext;
    this.timerService = timerService;
    this.flushConsolidationHandler = flushConsolidationHandler;
    this.idleTimeoutConsumer = idleTimeoutConsumer;
  }

  @Override
  public Connection listen(Consumer<IProtoResponse> listener) {
    log.debug("Added consumer: {}", listener.hashCode());
    this.consumer = listener;
    return this;
  }

  @Override
  public synchronized CompletableFuture<Greeting> connect(InetSocketAddress address, long timeoutMs)
      throws IllegalStateException {
    if (state.compareAndSet(State.CLOSED, State.CONNECTING)) {
      closedBy.set(CloseReason.NO_REASON);
      log.debug("{} is connecting to {}", this, address);

      CompletableFuture<Greeting> promise = new CompletableFuture<>();
      scheduleGreetingTimeout(promise, address, timeoutMs);
      connectPromise =
          promise.thenApply(
              greeting -> {
                cancelGreetingTimeout();
                log.info("{} connected to {}", this, address);
                this.greeting = greeting;
                state.set(State.READY);
                return greeting;
              });

      channel =
          bootstrap
              .handler(getInitializer(promise, sslContext, address))
              .remoteAddress(address)
              .connect()
              .addListener(onChannelConnect(promise, address))
              .channel();
    } else if (state.get() == State.CLOSING) {
      throw new IllegalStateException(state.get().getMessage());
    }

    return connectPromise;
  }

  @Override
  public CompletableFuture<Void> send(IProtoMessage msg) throws IllegalStateException {
    if (state.get() != State.READY) {
      String errorMessage;
      if (closedBy.get() != CloseReason.NO_REASON) {
        errorMessage = closedBy.get().getMessage();
      } else {
        errorMessage = state.get().getMessage();
      }
      throw new IllegalStateException(errorMessage);
    }

    CompletableFuture<Void> promise = new CompletableFuture<>();
    channel
        .writeAndFlush(msg)
        .addListener(
            f -> {
              if (f.isSuccess()) {
                log.debug("Message \"{}\" has been sent", msg);
                completePromise(promise, null);
                return;
              }

              failPromise(
                  promise,
                  new ConnectionException(String.format(ERR_SEND_FAILURE, msg), f.cause()));
            });

    return promise;
  }

  @Override
  public void close() {
    log.info("Connection closed by client {}", this);
    closeChannel(CloseReason.CLOSED_BY_CLIENT);
  }

  @Override
  public void shutdownClose() {
    log.info("Connection closed during shutdown {}", this);
    closeChannel(CloseReason.CLOSED_BY_SHUTDOWN);
  }

  private synchronized void closeChannel(CloseReason reason) {
    if (state.get() == State.CLOSED || state.get() == State.CLOSING) {
      return;
    }
    if (channel == null) {
      return;
    }

    closedBy.set(reason);
    state.set(State.CLOSING);
    channel.pipeline().close();
  }

  @Override
  public Optional<Greeting> getGreeting() {
    return Optional.ofNullable(greeting);
  }

  @Override
  public boolean isConnected() {
    return state.get() == State.READY;
  }

  @Override
  public Connection onClose(
      ConnectionCloseEvent event, BiConsumer<Connection, Throwable> callback) {
    if (!closeListeners.containsKey(event)) {
      closeListeners.put(event, new ArrayList<>());
    }
    closeListeners.get(event).add(callback);

    return this;
  }

  @Override
  public void pause() {
    log.debug("Pausing connection for channel ID {}", channel.id());
    channel.config().setAutoRead(false);
  }

  @Override
  public void resume() {
    log.debug("Resuming connection for channel ID {}", channel.id());
    channel.config().setAutoRead(true);
  }

  @Override
  public void setIdleTimeout(int idleTimeout) {
    this.idleTimeout = idleTimeout;
  }

  @Override
  public boolean isPaused() {
    return !this.channel.config().isAutoRead();
  }

  private ConnectionChannelInitializer getInitializer(
      CompletableFuture<Greeting> promise, SslContext sslContext, InetSocketAddress peerAddress) {
    ConnectionChannelInitializer.Builder initializerBuilder =
        new ConnectionChannelInitializer.Builder()
            .withConnectPromise(promise)
            .withMessageHandler(this::handleMessage)
            .withFlushConsolidationHandler(flushConsolidationHandler)
            .withCloseHandler(this::onChannelClose)
            .withIdleTimeout(this.idleTimeout);

    if (sslContext == null) {
      return initializerBuilder.build();
    }

    return initializerBuilder.withSSLContext(sslContext, peerAddress).build();
  }

  /**
   * This method returns callback for connectFuture to handle failures and exceptions which can
   * occur during connection process.
   *
   * @param promise greeting promise
   * @param address IP socket address
   * @return specified listener to the future
   */
  private ChannelFutureListener onChannelConnect(
      CompletableFuture<Greeting> promise, InetSocketAddress address) {
    return f -> {
      if (!f.isSuccess()) {
        failPromise(
            promise,
            new ConnectionException(
                String.format(closedBy.get().getMessage(), address), f.cause()));
        return;
      }

      state.compareAndSet(State.CONNECTING, State.WAITING);
    };
  }

  private synchronized void onChannelClose(ChannelFuture f) {
    String message;
    ConnectionCloseEvent event;
    CloseReason lastCloseReason = closedBy.get();
    if (lastCloseReason == CloseReason.CLOSED_BY_TIMEOUT) {
      // The connect promise is failed with TimeoutException by the greeting timer. The connection
      // has never been established, so there is no close event to fire.
      state.set(State.CLOSED);
      channel = null;
      connectPromise = null;
      return;
    }
    switch (lastCloseReason) {
      case CLOSED_BY_SHUTDOWN:
        message = lastCloseReason.getMessage();
        event = ConnectionCloseEvent.CLOSE_BY_SHUTDOWN;
        break;
      case CLOSED_BY_CLIENT:
        message = lastCloseReason.getMessage();
        event = ConnectionCloseEvent.CLOSE_BY_CLIENT;
        break;
      default:
        message = ERR_CLOSED_BY_SERVER;
        event = ConnectionCloseEvent.CLOSE_BY_REMOTE;
    }
    if (state.get() != State.CONNECTING) {
      Exception ex = new ConnectionClosedException(message, f.cause());
      failConnectPromise(ex);
      fire(event, ex);
    }
    state.set(State.CLOSED);
    channel = null;
    connectPromise = null;
  }

  private <T> void completePromise(CompletableFuture<T> promise, T result) {
    if (!promise.isDone()) {
      promise.complete(result);
    }
  }

  private <T> void failPromise(CompletableFuture<T> promise, Exception ex) {
    if (!promise.isDone()) {
      promise.completeExceptionally(ex);
    }
  }

  private void failConnectPromise(Exception ex) {
    if (connectPromise != null && !connectPromise.isDone()) {
      connectPromise.completeExceptionally(ex);
    }
  }

  private void fire(ConnectionCloseEvent event, Throwable ex) {
    if (!closeListeners.containsKey(event)) {
      return;
    }
    List<BiConsumer<Connection, Throwable>> handlers = closeListeners.get(event);
    for (BiConsumer<Connection, Throwable> handler : handlers) {
      handler.accept(this, ex);
    }
  }

  private void scheduleGreetingTimeout(
      CompletableFuture<Greeting> promise, InetSocketAddress address, long timeoutMs) {
    log.debug("Schedule timeout for connect in milliseconds: {}", timeoutMs);
    if (timeoutMs <= 0) {
      throw new IllegalStateException("Timeout must be greater than zero!");
    }
    timeoutHandler =
        this.timerService.newTimeout(
            timeoutHandler -> {
              if (closeOnGreetingTimeout(promise)) {
                promise.completeExceptionally(
                    new TimeoutException(String.format(ERR_GREETING_TIMEOUT, address, timeoutMs)));
              }
            },
            timeoutMs,
            TimeUnit.MILLISECONDS);
  }

  /**
   * Closes the channel if the greeting has not arrived yet.
   *
   * <p>The close reason is set before the channel is closed, so the close is not reported as a
   * close by server. The promise is completed by the caller outside the monitor.
   *
   * @param promise greeting promise
   * @return true if the channel is being closed because of the timeout
   */
  private synchronized boolean closeOnGreetingTimeout(CompletableFuture<Greeting> promise) {
    if (promise.isDone()) {
      return false;
    }
    if (state.get() != State.CONNECTING && state.get() != State.WAITING) {
      return false;
    }
    closeChannel(CloseReason.CLOSED_BY_TIMEOUT);
    return true;
  }

  private void cancelGreetingTimeout() {
    if (timeoutHandler == null) {
      return;
    }

    log.debug("Cancel timeout for timeoutHandler {}", timeoutHandler.hashCode());
    timeoutHandler.cancel();
    timeoutHandler = null;
  }

  private void handleMessage(IProtoResponse message, Throwable exc) {
    if (consumer == null || !isConnected()) {
      return;
    }
    if (exc instanceof IdleTimeoutException) {
      idleTimeoutConsumer.accept(this, exc);
      return;
    }
    if (message != null) {
      consumer.accept(message);
    }
  }
}

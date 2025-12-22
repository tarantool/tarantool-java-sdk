/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

import java.util.function.BiConsumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.Timer;

public class ConnectionFactory {

  private final Bootstrap bootstrap;
  private final Timer timerService;
  private SslContext sslContext;

  public ConnectionFactory(Bootstrap bootstrap, Timer timerService) {
    this.bootstrap = bootstrap;
    this.timerService = timerService;
  }

  public ConnectionFactory(Bootstrap bootstrap, SslContext sslContext, Timer timerService) {
    this.bootstrap = bootstrap;
    this.sslContext = sslContext;
    this.timerService = timerService;
  }

  public Connection create() {
    return new ConnectionImpl(this.bootstrap.clone(), sslContext, timerService, null);
  }

  public Connection create(
      FlushConsolidationHandler flushConsolidationHandler,
      BiConsumer<Connection, Throwable> idleTimeoutHandler) {
    return new ConnectionImpl(
        this.bootstrap.clone(),
        sslContext,
        timerService,
        flushConsolidationHandler,
        idleTimeoutHandler);
  }

  public Timer getTimerService() {
    return this.timerService;
  }

  public Bootstrap getBootstrap() {
    return bootstrap;
  }
}

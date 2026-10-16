/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

import java.time.Duration;
import java.util.function.Consumer;

import io.tarantool.core.TimeoutsUtils;

public class IProtoRequestOpts {

  private Consumer<IProtoMessage> pushHandler;
  private Handlers handlers;
  private Duration requestTimeout;
  private Long streamId;

  private IProtoRequestOpts() {
    this.requestTimeout = Duration.ofSeconds(3);
  }

  public static IProtoRequestOpts empty() {
    return new IProtoRequestOpts();
  }

  /**
   * @deprecated Use {@link #withRequestTimeout(Duration)}
   */
  @Deprecated
  public IProtoRequestOpts withRequestTimeout(long timeoutMs) {
    return withRequestTimeout(Duration.ofMillis(timeoutMs));
  }

  public IProtoRequestOpts withRequestTimeout(Duration timeout) {
    TimeoutsUtils.checkTimeoutIsPositive(timeout);
    this.requestTimeout = timeout;
    return this;
  }

  public IProtoRequestOpts withPushHandler(Consumer<IProtoMessage> callback) {
    this.pushHandler = callback;
    return this;
  }

  public IProtoRequestOpts withStreamId(Long streamId) {
    if (streamId != null && streamId < 0) {
      throw new IllegalArgumentException("streamId should be greater or equal 0");
    }
    this.streamId = streamId;
    return this;
  }

  /**
   * @deprecated Use {@link #requestTimeout()}
   */
  @Deprecated
  public long getRequestTimeout() {
    return this.requestTimeout.toMillis();
  }

  public Duration requestTimeout() {
    return this.requestTimeout;
  }

  public Long getStreamId() {
    return this.streamId;
  }

  public Consumer<IProtoMessage> getPushHandler() {
    return this.pushHandler;
  }

  public IProtoRequestOpts withHandlers(Handlers handlers) {
    this.handlers = handlers;
    return this;
  }

  public Handlers getHandlers() {
    return this.handlers;
  }
}

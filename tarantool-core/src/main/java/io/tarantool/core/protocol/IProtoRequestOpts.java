/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

import java.time.Duration;
import java.util.function.Consumer;

public class IProtoRequestOpts {

  private Consumer<IProtoMessage> pushHandler;
  private Handlers handlers;
  private Duration requestTimeout = Duration.ofSeconds(3);
  private Long streamId;

  private IProtoRequestOpts() {}

  public static IProtoRequestOpts empty() {
    return new IProtoRequestOpts();
  }

  /** <b><i>Use {@link #withRequestTimeout(Duration)} instead this.</i></b> */
  @Deprecated
  public IProtoRequestOpts withRequestTimeout(long timeoutMs) {
    if (timeoutMs <= 0) {
      throw new IllegalArgumentException("timeout should be greater than 0");
    }
    this.requestTimeout = Duration.ofMillis(timeoutMs);
    return this;
  }

  /**
   * Set up request timeout.
   *
   * @param timeout request timeout. Not null.
   */
  public IProtoRequestOpts withRequestTimeout(Duration timeout) {
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("timeout should be positive and  greater 0");
    }

    this.requestTimeout = timeout;
    return this;
  }

  public IProtoRequestOpts withPushHandler(Consumer<IProtoMessage> callback) {
    this.pushHandler = callback;
    return this;
  }

  /**
   * Set up stream id.
   *
   * @param streamId stream id. Must be not null and greater or equal than 0.
   */
  public IProtoRequestOpts withStreamId(Long streamId) {
    if (streamId != null && streamId < 0) {
      throw new IllegalArgumentException("streamId should be greater or equal 0");
    }
    this.streamId = streamId;
    return this;
  }

  public IProtoRequestOpts withHandlers(Handlers handlers) {
    this.handlers = handlers;
    return this;
  }

  /** Use {@link #requestTimeout()} instead this. */
  @Deprecated
  public long getRequestTimeout() {
    return this.requestTimeout.toMillis();
  }

  public Duration requestTimeout() {
    return this.requestTimeout;
  }

  /** Use {@link #streamId()} instead this. */
  @Deprecated
  public Long getStreamId() {
    return this.streamId;
  }

  public Long streamId() {
    return this.streamId;
  }

  /** Use {@link #pushHandler()} instead this. */
  @Deprecated
  public Consumer<IProtoMessage> getPushHandler() {
    return this.pushHandler;
  }

  public Consumer<IProtoMessage> pushHandler() {
    return this.pushHandler;
  }

  /** Use {@link #handlers()} instead this. */
  @Deprecated
  public Handlers getHandlers() {
    return this.handlers;
  }

  public Handlers handlers() {
    return this.handlers;
  }
}

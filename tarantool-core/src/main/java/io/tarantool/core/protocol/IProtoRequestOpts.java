/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

import java.util.function.Consumer;


public class IProtoRequestOpts {

  private Consumer<IProtoMessage> pushHandler;
  private long requestTimeoutMs;
  private Long streamId;

  private IProtoRequestOpts() {
    requestTimeoutMs = 3_000L;
  }

  public static IProtoRequestOpts empty() {
    return new IProtoRequestOpts();
  }

  public IProtoRequestOpts withRequestTimeout(long timeoutMs) {
    if (timeoutMs <= 0) {
      throw new IllegalArgumentException("timeout should be greater than 0");
    }
    this.requestTimeoutMs = timeoutMs;
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

  public long getRequestTimeout() {
    return this.requestTimeoutMs;
  }

  public Long getStreamId() {
    return this.streamId;
  }

  public Consumer<IProtoMessage> getPushHandler() {
    return this.pushHandler;
  }
}

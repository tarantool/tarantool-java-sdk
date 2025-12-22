/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core;

import java.util.function.BiConsumer;

public final class WatcherOptions {

  public static class Builder {

    private long sendTimeout = 5000;
    private BiConsumer<String, Throwable> errorHandler = (key, err) -> {};

    public Builder withErrorHandler(BiConsumer<String, Throwable> errback) {
      this.errorHandler = errback;
      return this;
    }

    public Builder withSendTimeout(long timeout) {
      this.sendTimeout = timeout;
      return this;
    }

    public WatcherOptions build() {
      return new WatcherOptions(sendTimeout, errorHandler);
    }
  }

  private long sendTimeout;
  private BiConsumer<String, Throwable> errorHandler;

  public static Builder builder() {
    return new Builder();
  }

  private WatcherOptions(long timeout, BiConsumer<String, Throwable> errback) {
    this.sendTimeout = timeout;
    this.errorHandler = errback;
  }

  public long getSendTimeout() {
    return sendTimeout;
  }

  public BiConsumer<String, Throwable> getErrorHandler() {
    return errorHandler;
  }
}

/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core;

import java.time.Duration;
import java.util.function.BiConsumer;

public final class WatcherOptions {

  private Duration sendTimeout;

  private BiConsumer<String, Throwable> errorHandler;

  private WatcherOptions(Duration timeout, BiConsumer<String, Throwable> errback) {
    this.sendTimeout = timeout;
    this.errorHandler = errback;
  }

  /**
   * @deprecated Use {@link #sendTimeout()} instead this
   */
  @Deprecated
  public long getSendTimeout() {
    return this.sendTimeout.toMillis();
  }

  public Duration sendTimeout() {
    return this.sendTimeout;
  }

  public BiConsumer<String, Throwable> getErrorHandler() {
    return errorHandler;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Duration sendTimeout = Duration.ofSeconds(5);

    private BiConsumer<String, Throwable> errorHandler = (key, err) -> {};

    public Builder withErrorHandler(BiConsumer<String, Throwable> errback) {
      this.errorHandler = errback;
      return this;
    }

    /**
     * @deprecated Use {@link #withSendTimeout(Duration)} instead this.
     */
    @Deprecated
    public Builder withSendTimeout(long timeout) {
      return withSendTimeout(Duration.ofMillis(timeout));
    }

    public Builder withSendTimeout(Duration timeout) {
      TimeoutsUtils.checkTimeoutIsPositive(timeout);
      this.sendTimeout = timeout;
      return this;
    }

    public WatcherOptions build() {
      return new WatcherOptions(sendTimeout, errorHandler);
    }
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

import java.util.function.Consumer;

/**
 * Container for request/response lifecycle handlers.
 *
 * <p>Used for tracing and monitoring request lifecycle:
 *
 * <ul>
 *   <li>{@link #onBeforeSend} - called before sending request
 *   <li>{@link #onSuccess} - called on successful response
 *   <li>{@link #onTimeout} - called when request times out
 *   <li>{@link #onIgnoredResponse} - called when response arrives after timeout
 * </ul>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 */
public class Handlers {

  private final Consumer<IProtoRequest> onBeforeSend;
  private final Consumer<IProtoResponse> onSuccess;
  private final Consumer<IProtoRequest> onTimeout;
  private final Consumer<IProtoResponse> onIgnoredResponse;

  private Handlers(Builder builder) {
    this.onBeforeSend = builder.onBeforeSend;
    this.onSuccess = builder.onSuccess;
    this.onTimeout = builder.onTimeout;
    this.onIgnoredResponse = builder.onIgnoredResponse;
  }

  /**
   * Creates a new builder for Handlers.
   *
   * @return Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns handler called before sending request.
   *
   * @return handler or null if not set
   */
  public Consumer<IProtoRequest> getOnBeforeSend() {
    return onBeforeSend;
  }

  /**
   * Returns handler called on successful response.
   *
   * @return handler or null if not set
   */
  public Consumer<IProtoResponse> getOnSuccess() {
    return onSuccess;
  }

  /**
   * Returns handler called when request times out.
   *
   * @return handler or null if not set
   */
  public Consumer<IProtoRequest> getOnTimeout() {
    return onTimeout;
  }

  /**
   * Returns handler called when response arrives after timeout.
   *
   * @return handler or null if not set
   */
  public Consumer<IProtoResponse> getOnIgnoredResponse() {
    return onIgnoredResponse;
  }

  /** Builder for Handlers. */
  public static class Builder {
    private Consumer<IProtoRequest> onBeforeSend;
    private Consumer<IProtoResponse> onSuccess;
    private Consumer<IProtoRequest> onTimeout;
    private Consumer<IProtoResponse> onIgnoredResponse;

    private Builder() {}

    /**
     * Sets handler called before sending request.
     *
     * @param handler the handler
     * @return this builder
     */
    public Builder onBeforeSend(Consumer<IProtoRequest> handler) {
      this.onBeforeSend = handler;
      return this;
    }

    /**
     * Sets handler called on successful response.
     *
     * @param handler the handler
     * @return this builder
     */
    public Builder onSuccess(Consumer<IProtoResponse> handler) {
      this.onSuccess = handler;
      return this;
    }

    /**
     * Sets handler called when request times out.
     *
     * @param handler the handler
     * @return this builder
     */
    public Builder onTimeout(Consumer<IProtoRequest> handler) {
      this.onTimeout = handler;
      return this;
    }

    /**
     * Sets handler called when response arrives after timeout.
     *
     * @param handler the handler
     * @return this builder
     */
    public Builder onIgnoredResponse(Consumer<IProtoResponse> handler) {
      this.onIgnoredResponse = handler;
      return this;
    }

    /**
     * Builds Handlers instance.
     *
     * @return new Handlers instance
     */
    public Handlers build() {
      return new Handlers(this);
    }
  }
}

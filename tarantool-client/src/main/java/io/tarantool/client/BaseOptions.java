/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client;

import java.time.Duration;

/**
 * The class implements base options for operations.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public class BaseOptions implements Options {

  /**
   * The time after which the request is considered invalid.
   *
   * <p>Default value: {@link #DEFAULT_TIMEOUT}.
   */
  private final Duration timeout;

  /**
   * Stream id for operation.
   *
   * <p>Default value: null.
   *
   * @see <a
   *     href="https://www.tarantool.io/ru/doc/latest/dev_guide/internals/iproto/streams/">Tarantool
   *     documentation</a>
   */
  private final Long streamId;

  /**
   * This constructor creates options based on the passed parameters.
   *
   * @param timeout see also: {@link #timeout}.
   * @param streamId see also: {@link #streamId}.
   * @see BaseOptions.Builder#build()
   */
  protected BaseOptions(Duration timeout, Long streamId) {
    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * Creates new builder for {@link BaseOptions} class.
   *
   * @return {@link BaseOptions} class builder object.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns timeout of operation.
   *
   * @return {@link BaseOptions#timeout} value in milliseconds.
   */
  @Deprecated
  public long getTimeout() {
    return this.timeout.toMillis();
  }

  @Override
  public Duration timeout() {
    return this.timeout;
  }

  /**
   * Returns stream id of operation.
   *
   * @return null - if {@link BaseOptions#streamId} is null, otherwise - {@link
   *     BaseOptions#streamId} value.
   */
  public Long getStreamId() {
    return this.streamId;
  }

  /** A specific builder for {@link BaseOptions} class. */
  public static class Builder {

    /**
     * @see BaseOptions#timeout
     */
    private Duration timeout = DEFAULT_TIMEOUT;

    /**
     * @see BaseOptions#streamId
     */
    private Long streamId;

    /**
     * Sets the {@link BaseOptions#timeout} parameter (in milliseconds) when constructing an
     * instance of a builder class. The following example creates a {@link BaseOptions} object with
     * a specified {@link BaseOptions#timeout} parameter:
     *
     * <pre>{@code
     * BaseOptions options = BaseOptions
     *                               .builder()
     *                               .withTimeout(2_000L)   // OK!
     *                               .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * BaseOptions options = BaseOptions
     *                              .builder()
     *                              .withTimeout(-1L) // Wrong! Throws exception!
     *                              .build();
     *
     *
     *
     * }</pre>
     *
     * @param timeout see {@link BaseOptions#timeout} field.
     * @return {@link BaseOptions.Builder} object.
     * @throws IllegalArgumentException when {@code timeout <= 0}.
     */
    @Deprecated
    public Builder withTimeout(long timeout) {
      return withTimeout(Duration.ofMillis(timeout));
    }

    /**
     * Sets the {@link BaseOptions#timeout} parameter when constructing an instance of a builder
     * class. The following example creates a {@link BaseOptions} object with a specified {@link
     * BaseOptions#timeout} parameter:
     *
     * <pre>{@code
     * BaseOptions options = BaseOptions
     *                               .builder()
     *                               .withTimeout(Duration.ofSeconds(2))   // OK!
     *                               .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * BaseOptions options = BaseOptions
     *                              .builder()
     *                              .withTimeout(Duration.ofSeconds(-1L)) // Wrong! Throws exception!
     *                              .build();
     *
     *
     *
     * }</pre>
     *
     * @param timeout see {@link BaseOptions#timeout} field.
     * @return {@link BaseOptions.Builder} object.
     * @throws IllegalArgumentException when {@code timeout <= 0}.
     */
    public Builder withTimeout(Duration timeout) {
      if (timeout.isNegative() || timeout.isZero()) {
        throw new IllegalArgumentException("timeout should be greater than 0");
      }
      this.timeout = timeout;
      return this;
    }

    /**
     * Sets the {@link BaseOptions#streamId} parameter when constructing an instance of a builder
     * class. The following example creates a {@link BaseOptions} object with a specified {@link
     * BaseOptions#streamId} parameter:
     *
     * <pre>{@code
     * BaseOptions options = BaseOptions
     *                               .builder()
     *                               .withStreamId(5L)   // OK!
     *                               .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * BaseOptions options = BaseOptions
     *                              .builder()
     *                              .withStreamId(-10L) // Wrong! Throws exception!
     *                              .build();
     *
     *
     *
     * }</pre>
     *
     * @param streamId see {@link BaseOptions#streamId} field.
     * @return {@link BaseOptions.Builder} object.
     * @throws IllegalArgumentException when {@code streamId < 0}.
     */
    public Builder withStreamId(long streamId) {
      if (streamId < 0) {
        throw new IllegalArgumentException("streamId should be greater or equal 0");
      }
      this.streamId = streamId;
      return this;
    }

    /**
     * Builds specific {@link BaseOptions} class instance with parameters.
     *
     * @return {@link BaseOptions} object.
     */
    public BaseOptions build() {
      return new BaseOptions(timeout, streamId);
    }
  }
}

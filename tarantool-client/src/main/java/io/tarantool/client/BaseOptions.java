/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client;

/**
 * <p>The class implements base options for operations.</p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public class BaseOptions implements Options {

  /**
   * Default {@link BaseOptions#timeout} value.
   */
  public static final long DEFAULT_TIMEOUT = 5_000;

  /**
   * <p> The time after which the request is considered invalid (in milliseconds).</p>
   * <p>Default value: {@value #DEFAULT_TIMEOUT} milliseconds.</p>
   */
  private final long timeout;

  /**
   * <p> Stream id for operation.</p>
   * <p> Default value: null.</p>
   *
   * @see <a href="https://www.tarantool.io/ru/doc/latest/dev_guide/internals/iproto/streams/">Tarantool
   * documentation</a>
   */
  private final Long streamId;

  /**
   * <p> This constructor creates options based on the passed parameters.</p>
   *
   * @param timeout  see also: {@link #timeout}.
   * @param streamId see also: {@link #streamId}.
   * @see BaseOptions.Builder#build()
   */
  protected BaseOptions(long timeout, Long streamId) {
    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * <p> Creates new builder for {@link BaseOptions} class.</p>
   *
   * @return {@link BaseOptions} class builder object.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * <p> Returns timeout of operation.</p>
   *
   * @return {@link BaseOptions#timeout} value in milliseconds.
   */
  public long getTimeout() {
    return this.timeout;
  }

  /**
   * <p> Returns stream id of operation.</p>
   *
   * @return null - if {@link BaseOptions#streamId} is null, otherwise - {@link BaseOptions#streamId} value.
   */
  public Long getStreamId() {
    return this.streamId;
  }

  /**
   * <p> A specific builder for {@link BaseOptions} class.</p>
   */
  static public class Builder {

    /**
     * @see BaseOptions#timeout
     */
    private long timeout = DEFAULT_TIMEOUT;

    /**
     * @see BaseOptions#streamId
     */
    private Long streamId;

    /**
     * <p> Sets the {@link BaseOptions#timeout} parameter (in milliseconds) when constructing an instance of a
     * builder class. The following example creates a {@link BaseOptions} object with a specified
     * {@link BaseOptions#timeout} parameter:
     * <pre>{@code
     *
     *
     *      BaseOptions options = BaseOptions
     *                                    .builder()
     *                                    .withTimeout(2_000L)   // OK!
     *                                    .build();
     *
     *
     *      }
     * </pre>
     * <pre>{@code
     *
     *
     *      BaseOptions options = BaseOptions
     *                                   .builder()
     *                                   .withTimeout(-1L) // Wrong! Throws exception!
     *                                   .build();
     *
     *
     *     }
     * </pre>
     *
     * @param timeout see {@link BaseOptions#timeout} field.
     * @return {@link BaseOptions.Builder} object.
     * @throws IllegalArgumentException when {@code timeout <= 0}.
     */
    public Builder withTimeout(long timeout) {
      if (timeout <= 0) {
        throw new IllegalArgumentException("timeout should be greater than 0");
      }
      this.timeout = timeout;
      return this;
    }

    /**
     * <p> Sets the {@link BaseOptions#streamId} parameter when constructing an instance of a builder class. The
     * following example creates a {@link BaseOptions} object with a specified {@link BaseOptions#streamId} parameter:
     * <pre>{@code
     *
     *
     *      BaseOptions options = BaseOptions
     *                                    .builder()
     *                                    .withStreamId(5L)   // OK!
     *                                    .build();
     *
     *
     *      }
     * </pre>
     * <pre>{@code
     *
     *
     *      BaseOptions options = BaseOptions
     *                                   .builder()
     *                                   .withStreamId(-10L) // Wrong! Throws exception!
     *                                   .build();
     *
     *
     *     }
     * </pre>
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
     * <p> Builds specific {@link BaseOptions} class instance with parameters.</p>
     *
     * @return {@link BaseOptions} object.
     */
    public BaseOptions build() {
      return new BaseOptions(timeout, streamId);
    }
  }
}

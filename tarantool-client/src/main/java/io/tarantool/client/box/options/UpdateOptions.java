/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.box.options;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.box.TarantoolBoxSpace;

/**
 * The class implements options for the update operation of the {@link TarantoolBoxClient
 * TarantoolBoxClient}.
 *
 * <p>Use this class to define a update operation options when using the {@link
 * TarantoolBoxSpace#update(List, List, UpdateOptions)}, {@link TarantoolBoxSpace#update(List, List,
 * UpdateOptions, TypeReference)}, {@link TarantoolBoxSpace#update(List, List, UpdateOptions,
 * Class)} API of {@link TarantoolBoxSpace}.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolBoxClient TarantoolBoxClient
 * @see TarantoolBoxSpace
 * @see TarantoolBoxSpace#update(List, List, UpdateOptions)
 * @see TarantoolBoxSpace#update(List, List, UpdateOptions, Class)
 * @see TarantoolBoxSpace#update(List, List, UpdateOptions, TypeReference)
 * @see <a
 *     href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_index/update/">Tarantool
 *     documentation</a>
 */
public class UpdateOptions implements OptionsWithIndex {

  /** Default {@link UpdateOptions#indexId} value. Primary index id has always 0. */
  public static final int PRIMARY = 0;

  /** Default {@link UpdateOptions#timeout} value. */
  public static final long DEFAULT_TIMEOUT = 5_000;

  /**
   * The time after which the request is considered invalid (in milliseconds).
   *
   * <p>Default value: {@value #DEFAULT_TIMEOUT} milliseconds.
   */
  private final long timeout;

  /**
   * Stream id for update operation.
   *
   * <p>Default value: null.
   *
   * @see <a
   *     href="https://www.tarantool.io/ru/doc/latest/dev_guide/internals/iproto/streams/">Tarantool
   *     documentation</a>
   */
  private final Long streamId;

  /**
   * Id of the index.
   *
   * <p>Default value: {@value #PRIMARY}.
   */
  private final int indexId;

  /** Name of the index. */
  private final String indexName;

  /**
   * This constructor creates options based on the passed parameters.
   *
   * @param timeout see also: {@link UpdateOptions#timeout}.
   * @param streamId see also: {@link UpdateOptions#streamId}.
   * @param indexId see also: {@link UpdateOptions#indexId}.
   * @param indexName see also: {@link UpdateOptions#indexName}.
   */
  private UpdateOptions(long timeout, Long streamId, int indexId, String indexName) {
    this.timeout = timeout;
    this.streamId = streamId;
    this.indexId = indexId;
    this.indexName = indexName;
  }

  /**
   * Creates new builder for {@link UpdateOptions} class.
   *
   * @return {@link UpdateOptions} class builder instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns timeout of update operation.
   *
   * @return {@link UpdateOptions#timeout} value in milliseconds.
   */
  public long getTimeout() {
    return this.timeout;
  }

  /**
   * Returns stream id of update operation.
   *
   * @return null - if {@link UpdateOptions#streamId} is null, otherwise - {@link
   *     UpdateOptions#streamId} value.
   */
  public Long getStreamId() {
    return this.streamId;
  }

  /**
   * Returns id of index.
   *
   * @return {@link UpdateOptions#indexId} value.
   */
  public int getIndexId() {
    return indexId;
  }

  /**
   * Returns name of index.
   *
   * @return {@link UpdateOptions#indexName} value.
   */
  public String getIndexName() {
    return indexName;
  }

  /** A specific builder for {@link UpdateOptions} class. */
  public static class Builder {

    /**
     * @see UpdateOptions#timeout
     */
    private long timeout = DEFAULT_TIMEOUT;

    /**
     * @see UpdateOptions#streamId
     */
    private Long streamId;

    /**
     * @see UpdateOptions#indexId
     */
    private int indexId = PRIMARY;

    /**
     * @see UpdateOptions#indexName
     */
    private String indexName;

    /**
     * Sets the {@link UpdateOptions#timeout} parameter (in milliseconds) when constructing an
     * instance of a builder class. The following example creates a {@link UpdateOptions} object
     * with a specified {@link UpdateOptions#timeout} parameter:
     *
     * <pre>{@code
     * UpdateOptions options = UpdateOptions
     *                               .builder()
     *                               .withTimeout(1_100L)   // OK!
     *                               .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * UpdateOptions options = UpdateOptions
     *                              .builder()
     *                              .withTimeout(-11L) // Wrong! Throws exception!
     *                              .build();
     *
     *
     *
     * }</pre>
     *
     * @param timeout see {@link UpdateOptions#timeout} field.
     * @return {@link UpdateOptions.Builder} instance.
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
     * Sets the {@link UpdateOptions#streamId} parameter when constructing an instance of a builder
     * class. The following example creates a {@link UpdateOptions} object with a specified {@link
     * UpdateOptions#streamId} parameter:
     *
     * <pre>{@code
     * UpdateOptions options = UpdateOptions
     *                               .builder()
     *                               .withStreamId(1L)   // OK!
     *                               .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * UpdateOptions options = UpdateOptions
     *                              .builder()
     *                              .withStreamId(-1L) // Wrong! Throws exception!
     *                              .build();
     *
     *
     *
     * }</pre>
     *
     * @param streamId see {@link UpdateOptions#streamId} field.
     * @return {@link Builder} object.
     * @throws IllegalArgumentException when {@code streamId < 0}.
     */

    // TODO https://github.com/tarantool/tarantool-java-ee/issues/266
    public Builder withStreamId(long streamId) {
      if (streamId < 0) {
        throw new IllegalArgumentException("streamId should be greater or equal 0");
      }
      this.streamId = streamId;
      return this;
    }

    /**
     * Sets the {@link UpdateOptions#indexId} parameter when constructing an instance of a builder
     * class. The following example creates a {@link UpdateOptions} object with a specified {@link
     * UpdateOptions#indexId} parameter:
     *
     * <pre>{@code
     * UpdateOptions options = UpdateOptions
     *                               .builder()
     *                               .withIndex(19)   // OK!
     *                               .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * UpdateOptions options = UpdateOptions
     *                              .builder()
     *                              .withIndex(-13) // Wrong! Throws exception!
     *                              .build();
     *
     *
     *
     * }</pre>
     *
     * @param indexId see {@link UpdateOptions#indexId} field.
     * @return {@link UpdateOptions.Builder} object.
     * @throws IllegalArgumentException when {@code indexId < 0}.
     */
    public Builder withIndex(int indexId) {
      if (indexId < 0) {
        throw new IllegalArgumentException("index should be greater or equal 0");
      }
      this.indexId = indexId;
      return this;
    }

    /**
     * Sets the {@link UpdateOptions#indexName} parameter when constructing an instance of a builder
     * class. The following example creates a {@link UpdateOptions} object with a specified {@link
     * UpdateOptions#indexName} parameter:
     *
     * <pre>{@code
     * UpdateOptions options = UpdateOptions
     *                               .builder()
     *                               .withIndex("pk")   // OK!
     *                               .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * UpdateOptions options = UpdateOptions
     *                              .builder()
     *                              .withIndex(null)   // Wrong! Throws exception!
     *                              .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * UpdateOptions options = UpdateOptions
     *                              .builder()
     *                              .withIndex(" ")    // Non compliant!
     *                              .build();
     *
     *
     *
     * }</pre>
     *
     * @param indexName see {@link UpdateOptions#indexName} field.
     * @return {@link UpdateOptions.Builder} object.
     * @throws IllegalArgumentException when {@code indexName == null}.
     */
    public Builder withIndex(String indexName) {
      if (indexName == null) {
        throw new IllegalArgumentException("index can't be null");
      }
      this.indexName = indexName;
      return this;
    }

    /**
     * Builds specific {@link UpdateOptions} class instance with parameters.
     *
     * @return {@link UpdateOptions} object.
     */
    public UpdateOptions build() {
      return new UpdateOptions(timeout, streamId, indexId, indexName);
    }
  }
}

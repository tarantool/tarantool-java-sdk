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
 * The class implements options for the delete operation of the {@link TarantoolBoxClient
 * TarantoolBoxClient}.
 *
 * <p>Use this class to define an options for delete operation when using the {@link
 * TarantoolBoxSpace#delete(List, DeleteOptions)}, {@link TarantoolBoxSpace#delete(List,
 * DeleteOptions, Class)}, {@link TarantoolBoxSpace#delete(List, DeleteOptions, TypeReference)} API
 * of {@link TarantoolBoxSpace}.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolBoxClient TarantoolBoxClient
 * @see TarantoolBoxSpace
 * @see TarantoolBoxSpace#delete(List, DeleteOptions)
 * @see TarantoolBoxSpace#delete(List, DeleteOptions, Class)
 * @see TarantoolBoxSpace#delete(List, DeleteOptions, TypeReference)
 * @see <a
 *     href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_index/delete/">Tarantool
 *     documentation</a>
 */
public class DeleteOptions implements OptionsWithIndex {

  /** Default {@link DeleteOptions#timeout} value. */
  public static final long DEFAULT_TIMEOUT = 5_000L;

  /** Default {@link DeleteOptions#indexId} value. Primary index id has always 0. */
  public static final int PRIMARY = 0;

  /**
   * The time after which the request is considered invalid (in milliseconds).
   *
   * <p>Default value: {@value #DEFAULT_TIMEOUT} milliseconds.
   */
  private final long timeout;

  /**
   * Stream id for delete operation.
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
   * Creates options based on the passed parameters.
   *
   * @param timeout see also: {@link DeleteOptions#timeout}.
   * @param streamId see also: {@link DeleteOptions#streamId}.
   * @param indexId see also: {@link DeleteOptions#indexId}.
   * @param indexName see also: {@link DeleteOptions#indexName}.
   * @see Builder#build()
   */
  private DeleteOptions(long timeout, Long streamId, int indexId, String indexName) {
    this.timeout = timeout;
    this.streamId = streamId;
    this.indexId = indexId;
    this.indexName = indexName;
  }

  /**
   * Creates new builder for {@link DeleteOptions} class.
   *
   * @return {@link DeleteOptions} class builder instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns timeout of delete operation.
   *
   * @return {@link DeleteOptions#timeout} value in milliseconds.
   */
  public long getTimeout() {
    return this.timeout;
  }

  /**
   * Returns stream id of delete operation.
   *
   * @return null - if {@link DeleteOptions#streamId} is null, otherwise - {@link
   *     DeleteOptions#streamId} value.
   */
  public Long getStreamId() {
    return this.streamId;
  }

  /**
   * Returns id of index.
   *
   * @return {@link DeleteOptions#indexId} value.
   */
  public int getIndexId() {
    return indexId;
  }

  /**
   * Returns index name.
   *
   * @return {@link DeleteOptions#indexName} value.
   */
  public String getIndexName() {
    return indexName;
  }

  /** A specific builder for {@link DeleteOptions} class. */
  public static class Builder {

    /**
     * @see DeleteOptions#timeout
     */
    private long timeout = DEFAULT_TIMEOUT;

    /**
     * @see DeleteOptions#streamId
     */
    private Long streamId;

    /**
     * @see DeleteOptions#indexId
     */
    private int indexId = PRIMARY;

    /**
     * @see DeleteOptions#indexName
     */
    private String indexName;

    /**
     * Sets the {@link DeleteOptions#timeout} parameter (in milliseconds) when constructing an
     * instance of a builder class. The following example creates a {@link DeleteOptions} object
     * with a specified {@link DeleteOptions#timeout} parameter:
     *
     * <pre>{@code
     * DeleteOptions options = DeleteOptions
     *                               .builder()
     *                               .withTimeout(5_000L)   // OK!
     *                               .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * DeleteOptions options = DeleteOptions
     *                              .builder()
     *                              .withTimeout(-1L) // Wrong! Throws exception!
     *                              .build();
     *
     *
     *
     * }</pre>
     *
     * @param timeout see {@link DeleteOptions#timeout} field.
     * @return {@link Builder} object.
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
     * Sets the {@link DeleteOptions#streamId} parameter when constructing an builder of a class.
     * The following example creates a {@link DeleteOptions} object with a specified {@link
     * DeleteOptions#streamId} parameter:
     *
     * <pre>{@code
     * DeleteOptions options = DeleteOptions
     *                             .builder()
     *                             .withStreamId(30L)   // OK!
     *                             .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * DeleteOptions options = DeleteOptions
     *                             .builder()
     *                             .withStreamId(-345L)   // Wrong! Throws exception!
     *                             .build();
     *
     *
     *
     * }</pre>
     *
     * @param streamId see {@link DeleteOptions#streamId} field.
     * @return {@link Builder} object.
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
     * Sets the {@link DeleteOptions#indexId} parameter when constructing an builder of a class. The
     * following example creates a {@link DeleteOptions} object with a specified {@link
     * DeleteOptions#indexId} parameter:
     *
     * <pre>{@code
     * DeleteOptions options = DeleteOptions
     *                              .builder()
     *                              .withIndex(5)    // OK!
     *                              .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * DeleteOptions options = DeleteOptions
     *                              .builder()
     *                              .withIndex(-25)   // Wrong! Throws exception!
     *                              .build();
     *
     *
     *
     * }</pre>
     *
     * @param indexId see {@link DeleteOptions#indexId} field.
     * @return {@link Builder} object.
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
     * Sets the {@link DeleteOptions#indexName} parameter when constructing an builder of a class.
     * The following example creates a {@link DeleteOptions} object with a specified {@link
     * DeleteOptions#indexName} parameter:
     *
     * <pre>{@code
     * DeleteOptions options = DeleteOptions
     *                             .builder()
     *                             .withIndex("pk")   // OK!
     *                             .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * DeleteOptions options = DeleteOptions
     *                             .builder()
     *                             .withIndex(null)   // Wrong! Throws exception!
     *                             .build();
     *
     *
     *
     * }</pre>
     *
     * <pre>{@code
     * DeleteOptions options = DeleteOptions
     *                             .builder()
     *                             .withIndex(" ")   //Non compliant!
     *                             .build();
     *
     *
     *
     * }</pre>
     *
     * @param indexName see {@link DeleteOptions#indexName} field.
     * @return {@link Builder} object.
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
     * Builds specific {@link DeleteOptions} class instance with parameters.
     *
     * @return {@link DeleteOptions} object.
     */
    public DeleteOptions build() {
      return new DeleteOptions(timeout, streamId, indexId, indexName);
    }
  }
}

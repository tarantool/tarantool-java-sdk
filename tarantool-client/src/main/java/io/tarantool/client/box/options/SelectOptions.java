/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.box.options;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.box.TarantoolBoxSpace;
import io.tarantool.core.protocol.BoxIterator;
import io.tarantool.mapping.SelectResponse;

/**
 * <p>
 * The class implements options for the select operation of the {@link TarantoolBoxClient TarantoolBoxClient}.
 * </p>
 * <p>
 * Use this class to define a select operation options when using the
 * {@link TarantoolBoxSpace#select(List, SelectOptions)},
 * {@link TarantoolBoxSpace#select(List, SelectOptions, TypeReference)},
 * {@link TarantoolBoxSpace#select(List, SelectOptions, Class)} API of {@link TarantoolBoxSpace}.
 * </p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolBoxClient TarantoolBoxClient
 * @see TarantoolBoxSpace
 * @see TarantoolBoxSpace#select(List, SelectOptions)
 * @see TarantoolBoxSpace#select(List, SelectOptions, TypeReference)
 * @see TarantoolBoxSpace#select(List, SelectOptions, Class)
 * @see <a href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_index/select/">Tarantool
 * documentation</a>
 */
public class SelectOptions implements OptionsWithIndex {

  /**
   * <p> Default {@link SelectOptions#limit} value.</p>
   */
  public static final int DEFAULT_LIMIT = 100;

  /**
   * <p> Default {@link SelectOptions#offset} value.</p>
   */
  public static final int DEFAULT_OFFSET = 0;

  /**
   * Default {@link SelectOptions#timeout} value.
   */
  public static final long DEFAULT_TIMEOUT = 5_000;

  /**
   * <p>Default {@link SelectOptions#iterator} value.</p>
   */
  public static final BoxIterator DEFAULT_BOX_ITERATOR = BoxIterator.EQ;

  /**
   * Default {@link SelectOptions#indexId} value. Primary index id has always 0.
   */
  public static final int PRIMARY = 0;

  /**
   * <p> The time after which the request is considered invalid (in milliseconds).</p>
   * <p>Default value: {@value #DEFAULT_TIMEOUT} milliseconds.</p>
   */
  private final long timeout;

  /**
   * <p> Stream id for select operation.</p>
   * <p> Default value: null.</p>
   *
   * @see <a href="https://www.tarantool.io/ru/doc/latest/dev_guide/internals/iproto/streams/">Tarantool
   * documentation</a>
   */
  private final Long streamId;

  /**
   * <p> Id of the index.</p>
   * <p>Default value: {@value #PRIMARY}.</p>
   */
  private final int indexId;

  /**
   * <p> Name of the index.</p>
   */
  private final String indexName;

  /**
   * <p>A maximum number of tuples for which the selection operation is carried out.</p>
   * <p>Default value: {@value #DEFAULT_LIMIT}.</p>
   *
   * @see <a href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_index/select/">Tarantool
   * documentation</a>
   */
  private final int limit;

  /**
   * <p> A number of tuples to skip (use this parameter carefully when scanning
   * <a href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_index/select/#offset-warning">large
   * data sets</a>.
   * </p>
   * <p> Default value: {@value #DEFAULT_OFFSET}.</p>
   *
   * @see <a href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_index/select/">Tarantool
   * documentation</a>
   */
  private final int offset;

  /**
   * <p>The {@link BoxIterator iterator} type. Returns an iterator that defines a selecting condition relative to
   * the tuple that was selected for selecting operation. The default iterator type is {@link BoxIterator#EQ 'EQ'}
   * .</p>
   *
   * @see <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_index/pairs/#box-index-iterator-types">Tarantool
   * documentation</a>
   * @see BoxIterator
   */
  private final BoxIterator iterator;

  /**
   * <p> if true, the select method returns the position of the last selected tuple.</p>
   * <p>Default value: false.</p>
   *
   * @see <a href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_index/select/">Tarantool
   * documentation</a>
   */
  private final boolean fetchPosition;

  /**
   * <p> After must contain a tuple from which selection must continue or its
   * position (a value from {@link SelectResponse#getPosition()}).</p>
   * <p>Default value: null.</p>
   *
   * @see <a href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_index/select/">Tarantool
   * documentation</a>
   */
  private final Object after;

  /**
   * <p> This constructor creates options based on the passed parameters.</p>
   *
   * @param timeout       see also: {@link #timeout}.
   * @param streamId      see also: {@link #streamId}.
   * @param indexId       see also: {@link #indexId}.
   * @param indexName     see also: {@link #indexName}.
   * @param limit         see also: {@link #limit}.
   * @param offset        see also: {@link  #offset}.
   * @param iterator      see also: {@link #iterator}.
   * @param fetchPosition see also: {@link #fetchPosition}.
   * @param after         see also: {@link #after}.
   * @see Builder#build()
   */
  private SelectOptions(long timeout, Long streamId, int indexId, String indexName, int limit, int offset,
      BoxIterator iterator, boolean fetchPosition, Object after) {
    this.timeout = timeout;
    this.streamId = streamId;
    this.indexId = indexId;
    this.indexName = indexName;
    this.limit = limit;
    this.offset = offset;
    this.iterator = iterator;
    this.fetchPosition = fetchPosition;
    this.after = after;
  }

  /**
   * <p> Creates new builder for {@link SelectOptions} class.</p>
   *
   * @return {@link SelectOptions} class builder object.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * <p> Returns timeout of select operation.</p>
   *
   * @return {@link SelectOptions#timeout} value in milliseconds.
   */
  public long getTimeout() {
    return this.timeout;
  }

  /**
   * <p> Returns stream id of select operation.</p>
   *
   * @return null - if {@link SelectOptions#streamId} is null, otherwise - {@link SelectOptions#streamId} value.
   */
  public Long getStreamId() {
    return this.streamId;
  }

  /**
   * <p> Returns id of index.</p>
   *
   * @return {@link #indexId} value.
   */
  public int getIndexId() {
    return indexId;
  }

  /**
   * Returns name of index.
   *
   * @return {@link #indexName} value.
   */
  public String getIndexName() {
    return indexName;
  }

  /**
   * Returns limit for selecting tuples.
   *
   * @return {@link #limit} value.
   */
  public int getLimit() {
    return limit;
  }

  /**
   * Returns offset (in tuple count) after which the tuple(s) will be selected.
   *
   * @return {@link #offset} value.
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Returns iterator for select operation.
   *
   * @return {@link #iterator} value.
   */
  public BoxIterator getIterator() {
    return iterator;
  }

  /**
   * Returns true if position of last tuple will be returned with the options.
   *
   * @return {@link #fetchPosition} value.
   */
  public boolean isPositionFetchEnabled() {
    return fetchPosition;
  }

  /**
   * Returns a tuple from which selection must continue or its position (a value from
   * {@link SelectResponse#getPosition()}.
   *
   * @return {@link #after} value.
   */
  public Object getAfter() {
    return after;
  }

  /**
   * <p> A specific builder for {@link SelectOptions} class.</p>
   */
  public static class Builder {

    /**
     * @see SelectOptions#timeout
     */
    private long timeout = DEFAULT_TIMEOUT;
    /**
     * @see SelectOptions#streamId
     */
    private Long streamId;
    /**
     * @see SelectOptions#indexId
     */
    private int indexId = PRIMARY;
    /**
     * @see SelectOptions#indexName
     */
    private String indexName;
    /**
     * @see SelectOptions#limit
     */
    private int limit = DEFAULT_LIMIT;
    /**
     * @see SelectOptions#offset
     */
    private int offset = DEFAULT_OFFSET;
    /**
     * @see SelectOptions#iterator
     */
    private BoxIterator iterator = DEFAULT_BOX_ITERATOR;
    /**
     * @see SelectOptions#fetchPosition
     */
    private boolean fetchPosition = false;
    /**
     * @see SelectOptions#after
     */
    private Object after = null;

    /**
     * <p> Sets the {@link SelectOptions#timeout} parameter (in milliseconds) when constructing an instance of a
     * builder class. The following example creates a {@link SelectOptions} object with a specified
     * {@link SelectOptions#timeout} parameter:
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                    .builder()
     *                                    .withTimeout(2_000L)   // OK!
     *                                    .build();
     *
     *
     *      }
     * </pre>
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                   .builder()
     *                                   .withTimeout(-1L) // Wrong! Throws exception!
     *                                   .build();
     *
     *
     *     }
     * </pre>
     *
     * @param timeout see {@link SelectOptions#timeout} field.
     * @return {@link SelectOptions.Builder} object.
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
     * <p> Sets the {@link SelectOptions#streamId} parameter when constructing an instance of a builder class. The
     * following example creates a {@link SelectOptions} object with a specified {@link SelectOptions#streamId}
     * parameter:
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                    .builder()
     *                                    .withStreamId(5L)   // OK!
     *                                    .build();
     *
     *
     *      }
     * </pre>
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                   .builder()
     *                                   .withStreamId(-10L) // Wrong! Throws exception!
     *                                   .build();
     *
     *
     *     }
     * </pre>
     *
     * @param streamId see {@link SelectOptions#streamId} field.
     * @return {@link SelectOptions.Builder} object.
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
     * <p> Sets the {@link SelectOptions#indexId} parameter when constructing an instance of a builder class. The
     * following example creates a {@link SelectOptions} object with a specified {@link SelectOptions#indexId}
     * parameter:
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                    .builder()
     *                                    .withIndex(5)   // OK!
     *                                    .build();
     *
     *
     *      }
     * </pre>
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                   .builder()
     *                                   .withIndex(-10)   // Wrong! Throws exception!
     *                                   .build();
     *
     *
     *     }
     * </pre>
     *
     * @param indexId see {@link SelectOptions#indexId} field.
     * @return {@link SelectOptions.Builder} object.
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
     * <p> Sets the {@link SelectOptions#indexName} parameter when constructing an instance of a builder class. The
     * following example creates a {@link SelectOptions} object with a specified {@link SelectOptions#indexName}
     * parameter:
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                    .builder()
     *                                    .withIndex("pk")   // OK!
     *                                    .build();
     *
     *
     *      }
     * </pre>
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                   .builder()
     *                                   .withIndex(null)   // Wrong! Throws exception!
     *                                   .build();
     *
     *
     *     }
     * </pre>
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                   .builder()
     *                                   .withIndex(" ")    // Non compliant!
     *                                   .build();
     *
     *
     *     }
     * </pre>
     *
     * @param indexName see {@link SelectOptions#indexName} field.
     * @return {@link SelectOptions.Builder} object.
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
     * <p> Sets the {@link SelectOptions#limit} parameter (in tuple count) when constructing an instance of a
     * builder class. The following example creates a {@link SelectOptions} object with a specified
     * {@link SelectOptions#limit} parameter:
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                    .builder()
     *                                    .withLimit(5)   // OK!
     *                                    .build();
     *
     *
     *      }
     * </pre>
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                   .builder()
     *                                   .withIndex(-10)   // OK! (Limit will be equals 0)
     *                                   .build();
     *
     *
     *     }
     * </pre>
     *
     * @param limit see {@link SelectOptions#limit} field.
     * @return {@link SelectOptions.Builder} object.
     */
    public Builder withLimit(int limit) {
      this.limit = limit;
      return this;
    }

    /**
     * <p> Sets the {@link SelectOptions#offset} parameter (in tuple count) when constructing an instance of a
     * builder class. The following example creates a {@link SelectOptions} object with a specified
     * {@link SelectOptions#offset} parameter:
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                    .builder()
     *                                    .withLimit(5)   // OK!
     *                                    .build();
     *
     *
     *      }
     * </pre>
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                   .builder()
     *                                   .withIndex(-10)   // OK!
     *                                   .build();
     *
     *
     *     }
     * </pre>
     *
     * @param offset see {@link SelectOptions#offset} field.
     * @return {@link SelectOptions.Builder} object.
     */
    public Builder withOffset(int offset) {
      this.offset = offset;
      return this;
    }

    /**
     * <p> Sets the {@link SelectOptions#iterator} parameter when constructing an instance of a builder class. The
     * following example creates a {@link SelectOptions} object with a specified {@link SelectOptions#iterator}
     * parameter:
     * <pre>
     *     {@code
     *
     *
     *      SelectOptions options = SelectOptions
     *                                    .builder()
     *                                    .withIterator(BoxIterator.EQ)   // OK!
     *                                    .build();
     *
     *
     *      }
     * </pre>
     *
     * @param iterator see {@link SelectOptions#iterator} field.
     * @return {@link SelectOptions.Builder} object.
     * @throws IllegalArgumentException when {@code iterator == null}.
     */

    // TODO Add `public Builder withIterator(String iterator) {`
    public Builder withIterator(BoxIterator iterator) {
      if (iterator == null) {
        throw new IllegalArgumentException("iterator can't be null");
      }
      this.iterator = iterator;
      return this;
    }

    /**
     * <p> Sets the {@link SelectOptions#fetchPosition} parameter to true when constructing an instance of a
     * builder class.
     * <pre>
     *     {@code
     *
     *      SelectOptions options = SelectOptions
     *                                    .builder()
     *                                    .fetchPosition()
     *                                    .build();
     *      SelectResponse<List<List<?>>> firstBatch = client.select(key, options).join();
     *
     *      assert(firstBatch.getPosition() != null);
     *      }
     * </pre>
     *
     * @return {@link SelectOptions.Builder} object.
     */
    public Builder fetchPosition() {
      this.fetchPosition = true;
      return this;
    }

    /**
     * <p> Sets the {@link SelectOptions#after} parameter to true when constructing an instance of a builder
     * class.
     * <pre>
     *     {@code
     *
     *
     *      SelectResponse<List<List<?>>> firstBatch = client.select(
     *              key, SelectOptions.builder().fetchPosition().build()).join();
     *
     *      You can use tuple as after argument:
     *
     *      SelectOptions options = SelectOptions
     *                                    .builder()
     *                                    .after(firstBatch.get().get(N))
     *                                    .build();
     *
     *      Or pass position from previous request:
     *
     *      SelectOptions options = SelectOptions
     *                                    .builder()
     *                                    .after(firstBatch.getPosition()) // firstBatch last tuple position
     *                                    .build();
     *
     *      SelectResponse<List<List<?>>> secondBatch = client.select(
     *             key, options).join();
     *
     *      }
     * </pre>
     *
     * @param after see {@link SelectOptions#after} field.
     * @return {@link SelectOptions.Builder} object.
     */
    public Builder after(Object after) {
      this.after = after;
      return this;
    }

    /**
     * <p> Builds specific {@link SelectOptions} class instance with parameters.</p>
     *
     * @return {@link SelectOptions} object.
     */
    public SelectOptions build() {
      return new SelectOptions(timeout, streamId, indexId, indexName, limit, offset, iterator, fetchPosition,
          after);
    }
  }
}

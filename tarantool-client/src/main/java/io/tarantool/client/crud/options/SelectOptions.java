/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.crud.options;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;

/**
 * The class implements options for the CRUD operation <a
 * href="https://github.com/tarantool/crud#select">select()</a>.
 *
 * <p>The following options are available:
 *
 * <p>Common options:
 *
 * <ul>
 *   <li>{@link #timeout}.
 *   <li>{@link #streamId}.
 * </ul>
 *
 * <p>Crud options:
 *
 * <ul>
 *   <li>{@value FIRST}.
 *   <li>{@value AFTER}.
 *   <li>{@value BATCH_SIZE}.
 *   <li>{@value BUCKET_ID}.
 *   <li>{@value FORCE_MAP_CALL}.
 *   <li>{@value TIMEOUT}.
 *   <li>{@value FIELDS}.
 *   <li>{@value FULLSCAN}.
 *   <li>{@value MODE}.
 *   <li>{@value PREFER_REPLICA}.
 *   <li>{@value BALANCE}.
 *   <li>{@value VSHARD_ROUTER}.
 *   <li>{@value YIELD_EVERY}.
 *   <li>{@value FETCH_LATEST_METADATA}.
 * </ul>
 *
 * <p>Examples:
 *
 * <blockquote>
 *
 * <pre>{@code
 * <<Example 1>>
 *
 *  TarantoolCrudSpace space = crudClient.space("spaceName");
 *  long timeout = 3_000L;
 *  Map<String, Object> options = new HashMap<String, Object>(){{
 *      put("timeout", 2_000L);
 *      // Determine which fields will be returned
 *      put("fields", Arrays.asList("id", "name"));
 *  }};
 *
 *  SelectOptions option = new SelectOptions(timeout, null, options);
 *
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  space.insert(Arrays.asList(1, true, "Vanya")).get();
 *
 *  // Selects tuple with id = 1 and with no conditions. Will return only the fields defined in the options.
 *  List<?> res = space.select(Collections.emptyList(), option);
 * }</pre>
 *
 * </blockquote>
 *
 * <blockquote>
 *
 * <pre>{@code
 * <<Example 2>>
 *
 * TarantoolCrudSpace space = crudClient.space("spaceName");
 *
 * SelectOptions option = SelectOptions.builder()
 *                                          .withTimeout(3_000L)
 *                                          .withCrudTimeout(2_000L)
 *                                          .withFields("id", "name")
 *                                          .build();
 *
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  space.insert(Arrays.asList(1, true, "Vanya")).get();
 *
 *  // Selects tuple with id = 1 and with no conditions. Will return only the fields defined in the options.
 *  List<?> res = space.select(Collections.emptyList(), option);
 * }</pre>
 *
 * </blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudClient
 * @see TarantoolCrudSpace
 */
public class SelectOptions implements CrudOptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(SelectOptions.class);

  /**
   * {@link TarantoolCrudClient#call(String) vshard.call} timeout and vshard master discovery
   * timeout (in milliseconds).
   *
   * <p><i><b>Note</b></i>: The time indicated by this parameter is the time between sending a
   * message from the router to Tarantool instance and the time when the answer will come from
   * Tarantool instance to router.
   */
  private static final String TIMEOUT = "timeout";

  /**
   * The maximum count of the objects to return. If negative value is specified, the objects behind
   * after are returned (after option is required in this case). <a
   * href="https://github.com/tarantool/crud/blob/master/doc/select.md#pagination">See pagination
   * examples</a>.
   */
  private static final String FIRST = "first";

  /** Tuple after which objects should be selected. */
  private static final String AFTER = "after";

  /** Number of tuples to process per one request to storage. */
  private static final String BATCH_SIZE = "batch_size";

  /** Bucket ID. */
  private static final String BUCKET_ID = "bucket_id";

  /**
   * if <tt>true</tt> then the map call is performed without any optimizations even if full primary
   * key equal condition is specified default value is <tt>false</tt>.
   */
  private static final String FORCE_MAP_CALL = "force_map_call";

  /** Field names for getting only a subset of fields. */
  private static final String FIELDS = "fields";

  /**
   * If <tt>true</tt> then a critical log entry will be skipped on potentially long select, see
   * avoiding <a
   * href="https://github.com/tarantool/crud/blob/master/doc/select.md#avoiding-full-scan">full
   * scan</a>.
   */
  private static final String FULLSCAN = "fullscan";

  /**
   * If {@link Mode#WRITE write} is specified then count is performed on master, default value is
   * {@link Mode#WRITE write}.
   */
  private static final String MODE = "mode";

  /**
   * If <tt>true</tt> then the preferred target is one of the replicas, default value is
   * <tt>false</tt>.
   */
  private static final String PREFER_REPLICA = "prefer_replica";

  /**
   * Use replica according to <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_rock/vshard/vshard_api/#router-api-call">vshard
   * load balancing policy</a>, default value is <tt>false</tt>.
   */
  private static final String BALANCE = "balance";

  /**
   * Cartridge vshard group name. Set this parameter if your space is not a part of the default
   * vshard cluster.
   */
  private static final String VSHARD_ROUTER = "vshard_router";

  /**
   * Number of tuples processed on storage to yield after, {@code yield_every should be > 0, default
   * value is 1000}.
   */
  private static final String YIELD_EVERY = "yield_every";

  /**
   * Guarantees the up-to-date metadata (space format) in first return value, otherwise it may not
   * take into account the latest migration of the data format. Performance overhead is up to 15%.
   * <tt>False</tt> by default .
   */
  private static final String FETCH_LATEST_METADATA = "fetch_latest_metadata";

  /** Default value for {@link #timeout}. */
  public static final long DEFAULT_TIMEOUT = 5_000;

  /**
   * The time after which the request is considered invalid (in milliseconds).
   *
   * <p>Default value: {@value #DEFAULT_TIMEOUT} milliseconds.
   *
   * <p><i><b>Note</b></i>: The time indicated by this parameter is the time between sending a
   * message from the connector to Tarantool and the time when the answer will come from Tarantool
   * to connector.
   */
  private final Long timeout;

  /**
   * Stream id for count operation.
   *
   * <p>Default value: {@code null}.
   *
   * @see <a
   *     href="https://www.tarantool.io/ru/doc/latest/dev_guide/internals/iproto/streams/">Tarantool
   *     documentation</a>
   */
  private final Long streamId;

  /** A map containing the correspondence between option names and their meanings. */
  private final Map<String, Object> crudOptions;

  /** Default {@link #FIRST} value. */
  public static final int DEFAULT_LIMIT = 100;

  /**
   * Creates a {@link SelectOptions} object with the given parameters.
   *
   * @param timeout {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options {@link #crudOptions}
   * @see SelectOptions
   */
  public SelectOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.crudOptions = options;

    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * Creates new builder instance of this class.
   *
   * @return {@link SelectOptions.Builder} object
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns value of timeout option.
   *
   * @return {@link #timeout} value.
   */
  @Override
  public long getTimeout() {
    return timeout;
  }

  /**
   * Returns value of stream id option.
   *
   * @return {@link #streamId} value.
   */
  @Override
  public Long getStreamId() {
    return streamId;
  }

  /**
   * Returns an immutable option map for the count operation.
   *
   * @return {@link Map} object.
   */
  @Override
  public Map<String, Object> getOptions() {
    return Collections.unmodifiableMap(crudOptions);
  }

  /**
   * Builder class for {@link SelectOptions}.
   *
   * @see SelectOptions
   */
  public static class Builder {

    /** See also: {@link SelectOptions#crudOptions}. */
    private final Map<String, Object> options = new HashMap<String, Object>();

    /** See also: {@link SelectOptions#timeout}. */
    private long timeout = DEFAULT_TIMEOUT;

    /** See also: {@link SelectOptions#streamId}. */
    private Long streamId;

    public Builder() {
      addOption(FIRST, DEFAULT_LIMIT);
      addOption(MODE, Mode.WRITE.value());
    }

    /**
     * Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.
     *
     * @param timeout value of timeout option.
     * @return {@link SelectOptions.Builder} object.
     * @throws IllegalArgumentException when timeout &#8804; 0.
     * @see SelectOptions#timeout
     * @see SelectOptions
     */
    public Builder withTimeout(long timeout) {
      if (timeout <= 0) {
        throw new IllegalArgumentException("timeout should be greater than 0");
      }
      this.timeout = timeout;
      return this;
    }

    /**
     * Sets value of {@link #streamId} option. StreamId parameter should be greater or equal 0.
     *
     * @param streamId value of stream id option
     * @return {@link SelectOptions.Builder} object
     * @throws IllegalArgumentException when streamId &#60; 0
     * @see SelectOptions#streamId
     * @see SelectOptions
     */
    public Builder withStreamId(Long streamId) {
      if (streamId < 0) {
        throw new IllegalArgumentException("streamId should be greater or equal 0");
      }
      this.streamId = streamId;
      return this;
    }

    /**
     * Sets value of {@link #TIMEOUT} option. Timeout parameter should be greater or equal 0.
     *
     * @param timeout value of {@link #TIMEOUT} option
     * @return {@link SelectOptions.Builder} object
     * @see #TIMEOUT
     * @see SelectOptions
     */
    public Builder withCrudTimeout(long timeout) {
      addOption(TIMEOUT, timeout);
      return this;
    }

    /**
     * Adds options by name into {@link #options} map. Name parameter should not be equal {@code
     * null}.
     *
     * @param name name of option
     * @param value value of option
     * @see SelectOptions
     * @see SelectOptions.Builder
     */
    private void addOption(String name, Object value) {
      if (name == null) {
        LOGGER.warn("Option isn't used since name of option is null, value = {}", value);
        return;
      }
      this.options.put(name, value);
    }

    /**
     * Sets value of {@link #FIRST} option.
     *
     * @param first value of {@link #FIRST} option
     * @return {@link SelectOptions.Builder} object
     * @see #FIRST
     * @see SelectOptions
     */
    public Builder withFirst(int first) {
      addOption(FIRST, first);
      return this;
    }

    /**
     * Sets value of {@link #AFTER} option.
     *
     * @param after value of {@link #AFTER} option
     * @return {@link SelectOptions.Builder} object
     * @see #AFTER
     * @see SelectOptions
     */
    public Builder withAfter(Object after) {
      addOption(AFTER, after);
      return this;
    }

    /**
     * Sets value of {@link #BATCH_SIZE} option.
     *
     * @param batchSize value of {@link #BATCH_SIZE} option
     * @return {@link SelectOptions.Builder} object
     * @see #BATCH_SIZE
     * @see SelectOptions
     */
    public Builder withBatchSize(Integer batchSize) {
      addOption(BATCH_SIZE, batchSize);
      return this;
    }

    /**
     * Sets value of {@link #BUCKET_ID} option. BucketId parameter should be greater or equal 0.
     *
     * @param bucketId value of {@link #BUCKET_ID} option
     * @return {@link SelectOptions.Builder} object
     * @see #BUCKET_ID
     * @see SelectOptions
     */
    public Builder withBucketId(Integer bucketId) {
      if (bucketId <= 0) {
        throw new IllegalArgumentException("bucketId should be greater 0");
      }
      addOption(BUCKET_ID, bucketId);
      return this;
    }

    /**
     * Sets value of {@link #FORCE_MAP_CALL} options to <tt>true</tt>. Default value -
     * <tt>false</tt>.
     *
     * @return {@link SelectOptions.Builder} object
     * @see #FORCE_MAP_CALL
     * @see SelectOptions
     */
    public Builder forceMapCall() {
      addOption(FORCE_MAP_CALL, true);
      return this;
    }

    /**
     * Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.
     *
     * @param fields value of {@link #FIELDS} option
     * @return {@link SelectOptions.Builder} object
     * @see #FIELDS
     * @see SelectOptions
     */
    public Builder withFields(String... fields) {
      addOption(FIELDS, Arrays.asList(fields));
      return this;
    }

    /**
     * Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.
     *
     * @param fields value of {@link #FIELDS} option
     * @return {@link SelectOptions.Builder} object
     * @see #FIELDS
     * @see SelectOptions
     */
    public Builder withFields(List<String> fields) {
      addOption(FIELDS, fields);
      return this;
    }

    /**
     * Sets value of {@link #FULLSCAN} option to <tt>true</tt>. Default value - <tt>false</tt>.
     *
     * @return {@link SelectOptions.Builder}
     * @see #FULLSCAN
     * @see SelectOptions
     */
    public Builder fullscan() {
      addOption(FULLSCAN, true);
      return this;
    }

    /**
     * Sets value of {@link #MODE} option. Default value - {@link Mode#WRITE}.
     *
     * @param mode value of {@link #MODE} option
     * @return {@link SelectOptions.Builder}
     * @see #MODE
     * @see SelectOptions
     * @see Mode
     */
    public Builder withMode(Mode mode) {
      addOption(MODE, mode.value());
      return this;
    }

    /**
     * Sets value of {@link #PREFER_REPLICA} option to <tt>true</tt>. Default value -
     * <tt>false</tt>.
     *
     * @return {@link SelectOptions.Builder}
     * @see #PREFER_REPLICA
     * @see SelectOptions
     */
    public Builder preferReplica() {
      addOption(PREFER_REPLICA, true);
      return this;
    }

    /**
     * Sets value of {@link #BALANCE} option to <tt>true</tt>. Default value - <tt>false</tt>.
     *
     * @return {@link SelectOptions.Builder}
     * @see #BALANCE
     * @see SelectOptions
     */
    public Builder balance() {
      addOption(BALANCE, true);
      return this;
    }

    /**
     * Sets value of {@link #VSHARD_ROUTER} option.
     *
     * @param vshardRouter value of {@link #VSHARD_ROUTER} option
     * @return {@link SelectOptions.Builder}
     * @see #VSHARD_ROUTER
     * @see SelectOptions
     */
    public Builder withVshardRouter(String vshardRouter) {
      addOption(VSHARD_ROUTER, vshardRouter);
      return this;
    }

    /**
     * Sets value of {@link #YIELD_EVERY} option. YieldEvery parameter should be greater 0. Default
     * value - 1_000.
     *
     * @param yieldEvery value of {@link #YIELD_EVERY} option
     * @return {@link SelectOptions.Builder}
     * @see #YIELD_EVERY
     * @see SelectOptions
     */
    public Builder withYieldEvery(Integer yieldEvery) {
      addOption(YIELD_EVERY, yieldEvery);
      return this;
    }

    /**
     * Sets value of {@link #FETCH_LATEST_METADATA} to <tt>true</tt>.
     *
     * @return {@link SelectOptions.Builder} object
     * @see #FETCH_LATEST_METADATA
     * @see SelectOptions
     */
    public Builder fetchLatestMetadata() {
      addOption(FETCH_LATEST_METADATA, true);
      return this;
    }

    /**
     * Sets options by name and value. OptionName parameter should not be equal {@code null}.
     *
     * @param optionName name of option
     * @param optionValue value of option
     * @return {@link SelectOptions.Builder}
     * @see SelectOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * Builds object of {@link SelectOptions} class.
     *
     * @return {@link SelectOptions} object
     * @see SelectOptions
     */
    public SelectOptions build() {
      return new SelectOptions(timeout, streamId, options);
    }
  }
}

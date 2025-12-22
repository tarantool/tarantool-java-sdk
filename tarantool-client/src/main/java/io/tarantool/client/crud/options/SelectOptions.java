/*
 * Copyright (c) 2025 VK Company Limited.
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
 * <p> The class implements options for the CRUD operation
 * <a href="https://github.com/tarantool/crud#select"> select()</a>.</p>
 * <p>The following options are available:</p>
 * <p>Common options:</p>
 * <ul>
 *     <li>{@link #timeout}.</li>
 *     <li>{@link #streamId}.</li>
 * </ul>
 * <p>Crud options:</p>
 * <ul>
 *     <li>{@value FIRST}.</li>
 *     <li>{@value AFTER}.</li>
 *     <li>{@value BATCH_SIZE}.</li>
 *     <li>{@value BUCKET_ID}.</li>
 *     <li>{@value FORCE_MAP_CALL}.</li>
 *     <li>{@value TIMEOUT}.</li>
 *     <li>{@value FIELDS}.</li>
 *     <li>{@value FULLSCAN}.</li>
 *     <li>{@value MODE}.</li>
 *     <li>{@value PREFER_REPLICA}.</li>
 *     <li>{@value BALANCE}.</li>
 *     <li>{@value VSHARD_ROUTER}.</li>
 *     <li>{@value YIELD_EVERY}.</li>
 *     <li>{@value FETCH_LATEST_METADATA}.</li>
 * </ul>
 * <p>Examples:</p>
 * <blockquote><pre>{@code
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
 * }</pre></blockquote>
 * <blockquote><pre>{@code
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
 * }</pre></blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudClient
 * @see TarantoolCrudSpace
 */
public class SelectOptions implements CrudOptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(SelectOptions.class);

  /**
   * <p>{@link TarantoolCrudClient#call(String) vshard.call} timeout and vshard master
   * discovery timeout (in milliseconds).</p>
   * <p><i><b>Note</b></i>: The time indicated by this parameter is the time between sending a message from the
   * router to Tarantool instance and the time when the answer will come from Tarantool instance to router.</p>
   */
  private static final String TIMEOUT = "timeout";

  /**
   * <p>The maximum count of the objects to return. If negative value is specified, the objects behind after are
   * returned (after option is required in this case).
   * <a href="https://github.com/tarantool/crud/blob/master/doc/select.md#pagination">See pagination
   * examples</a>.</p>
   */
  private static final String FIRST = "first";

  /**
   * <p>Tuple after which objects should be selected.</p>
   */
  private static final String AFTER = "after";

  /**
   * <p>Number of tuples to process per one request to storage.</p>
   */
  private static final String BATCH_SIZE = "batch_size";

  /**
   * <p>Bucket ID.</p>
   */
  private static final String BUCKET_ID = "bucket_id";

  /**
   * <p>if <tt>true</tt> then the map call is performed without any optimizations even if full primary key equal
   * condition is specified default value is <tt>false</tt>.</p>
   */
  private static final String FORCE_MAP_CALL = "force_map_call";

  /**
   * <p>Field names for getting only a subset of fields.</p>
   */
  private static final String FIELDS = "fields";

  /**
   * <p>If <tt>true</tt> then a critical log entry will be skipped on potentially long select, see avoiding
   * <a href="https://github.com/tarantool/crud/blob/master/doc/select.md#avoiding-full-scan">full scan</a>.</p>
   */
  private static final String FULLSCAN = "fullscan";

  /**
   * <p>If {@link Mode#WRITE write} is specified then count is performed on master, default value is {@link Mode#WRITE
   * write}.</p>
   */
  private static final String MODE = "mode";

  /**
   * <p>If <tt>true</tt> then the preferred target is one of the replicas, default value is <tt>false</tt>.</p>
   */
  private static final String PREFER_REPLICA = "prefer_replica";

  /**
   * <p>Use replica according to
   * <a href="https://www.tarantool.io/en/doc/latest/reference/reference_rock/vshard/vshard_api/#router-api-call">vshard
   * load balancing policy</a>, default value is <tt>false</tt>.</p>
   */
  private static final String BALANCE = "balance";

  /**
   * <p>Cartridge vshard group name. Set this parameter if your space is not a part of
   * the default vshard cluster.</p>
   */
  private static final String VSHARD_ROUTER = "vshard_router";

  /**
   * <p>Number of tuples processed on storage to yield after, {@code yield_every should be > 0, default value is
   * 1000}.</p>
   */
  private static final String YIELD_EVERY = "yield_every";

  /**
   * <p>Guarantees the up-to-date metadata (space format) in first return value, otherwise it may not take into
   * account the latest migration of the data format. Performance overhead is up to 15%. <tt>False</tt> by default
   * .</p>
   */
  private static final String FETCH_LATEST_METADATA = "fetch_latest_metadata";

  /**
   * <p>Default value for {@link #timeout}.</p>
   */
  public static final long DEFAULT_TIMEOUT = 5_000;

  /**
   * <p> The time after which the request is considered invalid (in milliseconds).</p>
   * <p>Default value: {@value #DEFAULT_TIMEOUT} milliseconds.</p>
   * <p><i><b>Note</b></i>: The time indicated by this parameter is the time between sending a message from the
   * connector to Tarantool and the time when the answer will come from Tarantool to connector.</p>
   */
  private final Long timeout;

  /**
   * <p> Stream id for count operation.</p>
   * <p> Default value: {@code null}.</p>
   *
   * @see <a href="https://www.tarantool.io/ru/doc/latest/dev_guide/internals/iproto/streams/">Tarantool
   * documentation</a>
   */
  private final Long streamId;

  /**
   * <p>A map containing the correspondence between option names and their meanings.</p>
   */
  private final Map<String, Object> crudOptions;

  /**
   * Default {@link #FIRST} value.
   */
  public static final int DEFAULT_LIMIT = 100;

  /**
   * <p>Creates a {@link SelectOptions} object with the given parameters.</p>
   *
   * @param timeout  {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options  {@link #crudOptions}
   * @see SelectOptions
   */
  public SelectOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.crudOptions = options;

    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * <p>Creates new builder instance of this class.</p>
   *
   * @return {@link SelectOptions.Builder} object
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * <p>Returns value of timeout option.</p>
   *
   * @return {@link #timeout} value.
   */
  @Override
  public long getTimeout() {
    return timeout;
  }

  /**
   * <p>Returns value of stream id option.</p>
   *
   * @return {@link #streamId} value.
   */
  @Override
  public Long getStreamId() {
    return streamId;
  }

  /**
   * <p>Returns an immutable option map for the count operation.</p>
   *
   * @return {@link Map} object.
   */
  @Override
  public Map<String, Object> getOptions() {
    return Collections.unmodifiableMap(crudOptions);
  }

  /**
   * <p>Builder class for {@link SelectOptions}.</p>
   *
   * @see SelectOptions
   */
  public static class Builder {

    /**
     * <p>See also: {@link SelectOptions#crudOptions}.</p>
     */
    private final Map<String, Object> options = new HashMap<String, Object>();

    /**
     * <p>See also: {@link SelectOptions#timeout}.</p>
     */
    private long timeout = DEFAULT_TIMEOUT;

    /**
     * <p>See also: {@link SelectOptions#streamId}.</p>
     */
    private Long streamId;

    public Builder() {
      addOption(FIRST, DEFAULT_LIMIT);
      addOption(MODE, Mode.WRITE.value());
    }

    /**
     * <p>Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.</p>
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
     * <p>Sets value of {@link #streamId} option. StreamId parameter should be greater or equal 0.</p>
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
     * <p>Sets value of {@link #TIMEOUT} option. Timeout parameter should be greater or equal 0.</p>
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
     * <p>Adds options by name into {@link #options} map. Name parameter should not be equal {@code null}.</p>
     *
     * @param name  name of option
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
     * <p>Sets value of {@link #FIRST} option.</p>
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
     * <p>Sets value of {@link #AFTER} option.</p>
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
     * <p>Sets value of {@link #BATCH_SIZE} option.</p>
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
     * <p>Sets value of {@link #BUCKET_ID} option. BucketId parameter should be greater or equal 0.</p>
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
     * <p>Sets value of {@link #FORCE_MAP_CALL} options to <tt>true</tt>. Default value - <tt>false</tt>.</p>
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
     * <p>Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.</p>
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
     * <p>Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.</p>
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
     * <p>Sets value of {@link #FULLSCAN} option to <tt>true</tt>. Default value - <tt>false</tt>.</p>
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
     * <p>Sets value of {@link #MODE} option. Default value - {@link Mode#WRITE}.</p>
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
     * <p>Sets value of {@link #PREFER_REPLICA} option to <tt>true</tt>. Default value - <tt>false</tt>.</p>
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
     * <p>Sets value of {@link #BALANCE} option to <tt>true</tt>. Default value - <tt>false</tt>.</p>
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
     * <p>Sets value of {@link #VSHARD_ROUTER} option.</p>
     *
     * @param vshardRouter value of {@link  #VSHARD_ROUTER} option
     * @return {@link SelectOptions.Builder}
     * @see #VSHARD_ROUTER
     * @see SelectOptions
     */
    public Builder withVshardRouter(String vshardRouter) {
      addOption(VSHARD_ROUTER, vshardRouter);
      return this;
    }

    /**
     * <p>Sets value of {@link #YIELD_EVERY} option. YieldEvery parameter should be greater 0. Default value -
     * 1_000.</p>
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
     * <p>Sets value of {@link #FETCH_LATEST_METADATA} to <tt>true</tt>.</p>
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
     * <p>Sets options by name and value. OptionName parameter should not be equal {@code null}.</p>
     *
     * @param optionName  name of option
     * @param optionValue value of option
     * @return {@link SelectOptions.Builder}
     * @see SelectOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * <p>Builds object of {@link SelectOptions} class.</p>
     *
     * @return {@link SelectOptions} object
     * @see SelectOptions
     */
    public SelectOptions build() {
      return new SelectOptions(timeout, streamId, options);
    }
  }
}

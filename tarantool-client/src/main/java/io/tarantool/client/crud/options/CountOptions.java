/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.crud.options;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.client.crud.Condition;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;

/**
 * The class implements options for the CRUD operation <a
 * href="https://github.com/tarantool/crud#count">count()</a>.
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
 *   <li>{@value YIELD_EVERY}.
 *   <li>{@value TIMEOUT}.
 *   <li>{@value BUCKET_ID}.
 *   <li>{@value FORCE_MAP_CALL}.
 *   <li>{@value FULLSCAN}.
 *   <li>{@value MODE}.
 *   <li>{@value PREFER_REPLICA}.
 *   <li>{@value BALANCE}.
 *   <li>{@value VSHARD_ROUTER}.
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
 *      put("mode", Mode.WRITE);
 *      put("timeout", 2_000L);
 *      put("balance", true);
 *  }};
 *
 *  Condition condition = Condition.create("==", "pk", 1);
 *  CountOptions options = new CountOptions(timeout, null, options);
 *
 *  int count = space.count(conditions, options).get();
 * }</pre>
 *
 * </blockquote>
 *
 * <blockquote>
 *
 * <pre>{@code
 * <<Example 2>>
 *
 *  TarantoolCrudSpace space = crudClient.space("spaceName");
 *
 *  Condition condition = Condition.create("==", "pk", 1);
 *  CountOptions options = CountOptions.builder()
 *                                          .withTimeout(3_000L)
 *                                          .withStreamId(null)
 *                                          .withCrudTimeout(2_000L)
 *                                          .withMode(Mode.WRITE)
 *                                          .balance()
 *                                          .build();
 *
 *  int count = space.count(conditions, options).get();
 * }</pre>
 *
 * </blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudClient
 * @see TarantoolCrudSpace
 * @see Condition
 */
public class CountOptions implements CrudOptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(CountOptions.class);

  /**
   * Number of tuples processed to yield after, {@value YIELD_EVERY} should be &#62; 0, default
   * value is 1_000.
   */
  private static final String YIELD_EVERY = "yield_every";

  /**
   * {@link TarantoolCrudClient#call(String) vshard.call} timeout and vshard master discovery
   * timeout (in milliseconds).
   *
   * <p><i><b>Note</b></i>: The time indicated by this parameter is the time between sending a
   * message from the router to Tarantool instance and the time when the answer will come from
   * Tarantool instance to router.
   */
  private static final String TIMEOUT = "timeout";

  /** Bucket ID. */
  private static final String BUCKET_ID = "bucket_id";

  /**
   * If <tt>true</tt> then the map call is performed without any optimizations even, default value
   * is <tt>false</tt>.
   */
  private static final String FORCE_MAP_CALL = "force_map_call";

  /**
   * If <tt>true</tt> then a critical log entry will be skipped on potentially long count, see
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

  /**
   * Creates a {@link CountOptions} object with the given parameters.
   *
   * @param timeout {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options {@link #crudOptions}
   * @see CountOptions
   */
  public CountOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.crudOptions = options;

    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * Creates new builder instance of this class.
   *
   * @return {@link Builder} object
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
   * Builder class for {@link CountOptions}.
   *
   * @see CountOptions
   */
  public static class Builder {

    /** See also: {@link CountOptions#crudOptions}. */
    private final Map<String, Object> options = new HashMap<>();

    /** See also: {@link CountOptions#timeout}. */
    private long timeout = DEFAULT_TIMEOUT;

    /** See also: {@link CountOptions#streamId}. */
    private Long streamId;

    public Builder() {
      addOption(MODE, Mode.WRITE.value());
    }

    /**
     * Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.
     *
     * @param timeout value of timeout option.
     * @return {@link Builder} object.
     * @throws IllegalArgumentException when timeout &#8804; 0.
     * @see CountOptions#timeout
     * @see CountOptions
     */
    public Builder withTimeout(long timeout) throws IllegalArgumentException {
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
     * @return {@link Builder} object
     * @throws IllegalArgumentException when streamId &#60; 0
     * @see CountOptions#streamId
     * @see CountOptions
     */
    public Builder withStreamId(Long streamId) throws IllegalArgumentException {
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
     * @return {@link Builder} object
     * @see #TIMEOUT
     * @see CountOptions
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
     * @see CountOptions
     * @see Builder
     */
    private void addOption(String name, Object value) {
      if (name == null) {
        LOGGER.warn("Option isn't used since name of option is null, value = {}", value);
        return;
      }
      this.options.put(name, value);
    }

    /**
     * Sets value of {@link #BUCKET_ID} option. BucketId parameter should be greater or equal 0.
     *
     * @param bucketId value of {@link #BUCKET_ID} option
     * @return {@link Builder} object
     * @see #BUCKET_ID
     * @see CountOptions
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
     * @return {@link Builder} object
     * @see #FORCE_MAP_CALL
     * @see CountOptions
     */
    public Builder forceMapCall() {
      addOption(FORCE_MAP_CALL, true);
      return this;
    }

    /**
     * Sets value of {@link #FULLSCAN} option to <tt>true</tt>. Default value - <tt>false</tt>.
     *
     * @return {@link Builder}
     * @see #FULLSCAN
     * @see CountOptions
     */
    public Builder fullscan() {
      addOption(FULLSCAN, true);
      return this;
    }

    /**
     * Sets value of {@link #MODE} option. Default value - {@link Mode#WRITE}.
     *
     * @param mode value of {@link #MODE} option
     * @return {@link Builder}
     * @see #MODE
     * @see CountOptions
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
     * @return {@link Builder}
     * @see #PREFER_REPLICA
     * @see CountOptions
     */
    public Builder preferReplica() {
      addOption(PREFER_REPLICA, true);
      return this;
    }

    /**
     * Sets value of {@link #BALANCE} option to <tt>true</tt>. Default value - <tt>false</tt>.
     *
     * @return {@link Builder}
     * @see #BALANCE
     * @see CountOptions
     */
    public Builder balance() {
      addOption(BALANCE, true);
      return this;
    }

    /**
     * Sets value of {@link #VSHARD_ROUTER} option.
     *
     * @param vshardRouter value of {@link #VSHARD_ROUTER} option
     * @return {@link Builder}
     * @see #VSHARD_ROUTER
     * @see CountOptions
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
     * @return {@link Builder}
     * @see #YIELD_EVERY
     * @see CountOptions
     */
    public Builder withYieldEvery(Integer yieldEvery) {
      addOption(YIELD_EVERY, yieldEvery);
      return this;
    }

    /**
     * Sets options by name and value. OptionName parameter should not be equal {@code null}.
     *
     * @param optionName name of option
     * @param optionValue value of option
     * @return {@link Builder}
     * @see CountOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * Builds object of {@link CountOptions} class.
     *
     * @return {@link CountOptions} object
     * @see CountOptions
     */
    public CountOptions build() {
      return new CountOptions(timeout, streamId, options);
    }
  }
}

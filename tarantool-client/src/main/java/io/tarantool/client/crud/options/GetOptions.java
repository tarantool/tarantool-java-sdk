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
 * href="https://github.com/tarantool/crud#get">get()</a>.
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
 *   <li>{@value TIMEOUT}.
 *   <li>{@value BUCKET_ID}.
 *   <li>{@value FIELDS}.
 *   <li>{@value PREFER_REPLICA}.
 *   <li>{@value MODE}.
 *   <li>{@value VSHARD_ROUTER}.
 *   <li>{@value FETCH_LATEST_METADATA}.
 *   <li>{@value BALANCE}.
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
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  space.insert(Arrays.asList(1, true, "Vanya"));
 *
 *  GetOptions options = new GetOptions(timeout, null, options);
 *
 *  // Returns list with one tuple as list [1, "Vanya"]
 *  List<?> res = space.get(Collections.singletonList(1), options).get();
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
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  space.insert(Arrays.asList(1, true, "Vanya"));
 *
 *  GetOptions options = GetOptions.builder()
 *                                   .withTimeout(3_000L)
 *                                   .withCrudTimeout(2_000L)
 *                                   .withFields("id", "name")
 *                                   .build();
 *
 *  // Returns list with one tuple as list [1, "Vanya"]
 *  List<?> res = space.get(Collections.singletonList(1), options).get();
 * }</pre>
 *
 * </blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudClient
 * @see TarantoolCrudSpace
 */
public class GetOptions implements CrudOptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(GetOptions.class);

  /** Field names for getting only a subset of fields. */
  private static final String FIELDS = "fields";

  /** Bucket ID. */
  private static final String BUCKET_ID = "bucket_id";

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

  /**
   * Creates a {@link GetOptions} object with the given parameters.
   *
   * @param timeout {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options {@link #crudOptions}
   * @see GetOptions
   */
  public GetOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.crudOptions = options;

    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * Creates new builder instance of this class.
   *
   * @return {@link GetOptions.Builder} object
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
   * Builder class for {@link GetOptions}.
   *
   * @see GetOptions
   */
  public static class Builder {

    /** See also: {@link GetOptions#crudOptions}. */
    private final Map<String, Object> options = new HashMap<>();

    /** See also: {@link GetOptions#timeout}. */
    private long timeout = DEFAULT_TIMEOUT;

    /** See also: {@link GetOptions#streamId}. */
    private Long streamId;

    public Builder() {
      addOption(MODE, Mode.WRITE.value());
    }

    /**
     * Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.
     *
     * @param timeout value of timeout option.
     * @return {@link GetOptions.Builder} object.
     * @throws IllegalArgumentException when timeout &#8804; 0.
     * @see GetOptions#timeout
     * @see GetOptions
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
     * @return {@link GetOptions.Builder} object
     * @throws IllegalArgumentException when streamId &#60; 0
     * @see GetOptions#streamId
     * @see GetOptions
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
     * @return {@link GetOptions.Builder} object
     * @see #TIMEOUT
     * @see GetOptions
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
     * @see GetOptions
     * @see GetOptions.Builder
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
     * @return {@link GetOptions.Builder} object
     * @see #BUCKET_ID
     * @see GetOptions
     */
    public Builder withBucketId(Integer bucketId) {
      if (bucketId <= 0) {
        throw new IllegalArgumentException("bucketId should be greater 0");
      }
      addOption(BUCKET_ID, bucketId);
      return this;
    }

    /**
     * Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.
     *
     * @param fields value of {@link #FIELDS} option
     * @return {@link GetOptions.Builder} object
     * @see #FIELDS
     * @see GetOptions
     */
    public Builder withFields(String... fields) {
      addOption(FIELDS, Arrays.asList(fields));
      return this;
    }

    /**
     * Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.
     *
     * @param fields value of {@link #FIELDS} option
     * @return {@link GetOptions.Builder} object
     * @see #FIELDS
     * @see GetOptions
     */
    public Builder withFields(List<String> fields) {
      addOption(FIELDS, fields);
      return this;
    }

    /**
     * Sets value of {@link #MODE} option. Default value - {@link Mode#WRITE}.
     *
     * @param mode value of {@link #MODE} option
     * @return {@link GetOptions.Builder}
     * @see #MODE
     * @see GetOptions
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
     * @return {@link GetOptions.Builder}
     * @see #PREFER_REPLICA
     * @see GetOptions
     */
    public Builder preferReplica() {
      addOption(PREFER_REPLICA, true);
      return this;
    }

    /**
     * Sets value of {@link #BALANCE} option to <tt>true</tt>. Default value - <tt>false</tt>.
     *
     * @return {@link GetOptions.Builder}
     * @see #BALANCE
     * @see GetOptions
     */
    public Builder balance() {
      addOption(BALANCE, true);
      return this;
    }

    /**
     * Sets value of {@link #VSHARD_ROUTER} option.
     *
     * @param vshardRouter value of {@link #VSHARD_ROUTER} option
     * @return {@link GetOptions.Builder}
     * @see #VSHARD_ROUTER
     * @see GetOptions
     */
    public Builder withVshardRouter(String vshardRouter) {
      addOption(VSHARD_ROUTER, vshardRouter);
      return this;
    }

    /**
     * Sets value of {@link #FETCH_LATEST_METADATA} to <tt>true</tt>.
     *
     * @return {@link GetOptions.Builder} object
     * @see #FETCH_LATEST_METADATA
     * @see GetOptions
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
     * @return {@link GetOptions.Builder}
     * @see GetOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * Builds object of {@link GetOptions} class.
     *
     * @return {@link GetOptions} object
     * @see GetOptions
     */
    public GetOptions build() {
      return new GetOptions(timeout, streamId, options);
    }
  }
}

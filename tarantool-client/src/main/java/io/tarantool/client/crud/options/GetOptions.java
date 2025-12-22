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
 * <a href="https://github.com/tarantool/crud#get"> get()</a>.</p>
 * <p>The following options are available:</p>
 * <p>Common options:</p>
 * <ul>
 *     <li>{@link #timeout}.</li>
 *     <li>{@link #streamId}.</li>
 * </ul>
 * <p>Crud options:</p>
 * <ul>
 * <li>{@value TIMEOUT}.</li>
 * <li>{@value BUCKET_ID}.</li>
 * <li>{@value FIELDS}.</li>
 * <li>{@value PREFER_REPLICA}.</li>
 * <li>{@value MODE}.</li>
 * <li>{@value VSHARD_ROUTER}.</li>
 * <li>{@value FETCH_LATEST_METADATA}.</li>
 * <li>{@value BALANCE}.</li>
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
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  space.insert(Arrays.asList(1, true, "Vanya"));
 *
 *  GetOptions options = new GetOptions(timeout, null, options);
 *
 *  // Returns list with one tuple as list [1, "Vanya"]
 *  List<?> res = space.get(Collections.singletonList(1), options).get();
 * }</pre></blockquote>
 * <blockquote><pre>{@code
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
 * }</pre></blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudClient
 * @see TarantoolCrudSpace
 */
public class GetOptions implements CrudOptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(GetOptions.class);

  /**
   * <p>Field names for getting only a subset of fields.</p>
   */
  private static final String FIELDS = "fields";

  /**
   * <p>Bucket ID.</p>
   */
  private static final String BUCKET_ID = "bucket_id";

  /**
   * <p>{@link TarantoolCrudClient#call(String) vshard.call} timeout and vshard master
   * discovery timeout (in milliseconds).</p>
   * <p><i><b>Note</b></i>: The time indicated by this parameter is the time between sending a message from the
   * router to Tarantool instance and the time when the answer will come from Tarantool instance to router.</p>
   */
  private static final String TIMEOUT = "timeout";

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
   * <p>Creates a {@link GetOptions} object with the given parameters.</p>
   *
   * @param timeout  {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options  {@link #crudOptions}
   * @see GetOptions
   */
  public GetOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.crudOptions = options;

    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * <p>Creates new builder instance of this class.</p>
   *
   * @return {@link GetOptions.Builder} object
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
   * <p>Builder class for {@link GetOptions}.</p>
   *
   * @see GetOptions
   */
  public static class Builder {

    /**
     * <p>See also: {@link GetOptions#crudOptions}.</p>
     */
    private final Map<String, Object> options = new HashMap<>();

    /**
     * <p>See also: {@link GetOptions#timeout}.</p>
     */
    private long timeout = DEFAULT_TIMEOUT;

    /**
     * <p>See also: {@link GetOptions#streamId}.</p>
     */
    private Long streamId;

    public Builder() {
      addOption(MODE, Mode.WRITE.value());
    }

    /**
     * <p>Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.</p>
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
     * <p>Sets value of {@link #streamId} option. StreamId parameter should be greater or equal 0.</p>
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
     * <p>Sets value of {@link #TIMEOUT} option. Timeout parameter should be greater or equal 0.</p>
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
     * <p>Adds options by name into {@link #options} map. Name parameter should not be equal {@code null}.</p>
     *
     * @param name  name of option
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
     * <p>Sets value of {@link #BUCKET_ID} option. BucketId parameter should be greater or equal 0.</p>
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
     * <p>Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.</p>
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
     * <p>Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.</p>
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
     * <p>Sets value of {@link #MODE} option. Default value - {@link Mode#WRITE}.</p>
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
     * <p>Sets value of {@link #PREFER_REPLICA} option to <tt>true</tt>. Default value - <tt>false</tt>.</p>
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
     * <p>Sets value of {@link #BALANCE} option to <tt>true</tt>. Default value - <tt>false</tt>.</p>
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
     * <p>Sets value of {@link #VSHARD_ROUTER} option.</p>
     *
     * @param vshardRouter value of {@link  #VSHARD_ROUTER} option
     * @return {@link GetOptions.Builder}
     * @see #VSHARD_ROUTER
     * @see GetOptions
     */
    public Builder withVshardRouter(String vshardRouter) {
      addOption(VSHARD_ROUTER, vshardRouter);
      return this;
    }

    /**
     * <p>Sets value of {@link #FETCH_LATEST_METADATA} to <tt>true</tt>.</p>
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
     * <p>Sets options by name and value. OptionName parameter should not be equal {@code null}.</p>
     *
     * @param optionName  name of option
     * @param optionValue value of option
     * @return {@link GetOptions.Builder}
     * @see GetOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * <p>Builds object of {@link GetOptions} class.</p>
     *
     * @return {@link GetOptions} object
     * @see GetOptions
     */
    public GetOptions build() {
      return new GetOptions(timeout, streamId, options);
    }
  }
}

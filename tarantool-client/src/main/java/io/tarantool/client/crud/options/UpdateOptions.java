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
 * href="https://github.com/tarantool/crud#update">update()</a>.
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
 *   <li>{@value NO_RETURN}.
 *   <li>{@value BUCKET_ID}.
 *   <li>{@value TIMEOUT}.
 *   <li>{@value FIELDS}.
 *   <li>{@value VSHARD_ROUTER}.
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
 *  UpdateOptions option = new UpdateOptions(timeout, null, options);
 *
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  space.insert(Arrays.asList(1, true, "Vanya")).get();
 *
 *  // Updates tuple (id = 1, isMarried = true, name = "Vanya") to (id = 1, isMarried = true, name = "Petya")
 *  // Returns a tuple with the fields defined in update option
 *  List<?> res = space.update(Collections.singletonList(1), Arrays.asList("=", "name", "Petya"), option);
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
 *  // Updates tuple (id = 1, isMarried = true, name = "Vanya") to (id = 1, isMarried = true, name = "Petya")
 *  // Returns a tuple with the fields defined in update option (1, "Petya")
 *  List<?> res = space.update(Collections.singletonList(1), Arrays.asList("=", "name", "Petya"), option);
 * }</pre>
 *
 * </blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudClient
 * @see TarantoolCrudSpace
 */
public class UpdateOptions implements CrudOptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateOptions.class);

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

  /** Field names for getting only a subset of fields. */
  private static final String FIELDS = "fields";

  /**
   * Cartridge vshard group name. Set this parameter if your space is not a part of the default
   * vshard cluster.
   */
  private static final String VSHARD_ROUTER = "vshard_router";

  /**
   * Suppress successfully processed tuple (first return value is nil (null)). <tt>False</tt> by
   * default.
   */
  private static final String NO_RETURN = "noreturn";

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
   * Creates a {@link UpdateOptions} object with the given parameters.
   *
   * @param timeout {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options {@link #crudOptions}
   * @see UpdateOptions
   */
  public UpdateOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.crudOptions = options;

    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * Creates new builder instance of this class.
   *
   * @return {@link UpdateOptions.Builder} object
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
   * Builder class for {@link UpdateOptions}.
   *
   * @see UpdateOptions
   */
  public static class Builder {

    /** See also: {@link UpdateOptions#crudOptions}. */
    private final Map<String, Object> options = new HashMap<>();

    /** See also: {@link UpdateOptions#timeout}. */
    private long timeout = DEFAULT_TIMEOUT;

    /** See also: {@link UpdateOptions#streamId}. */
    private Long streamId;

    /**
     * Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.
     *
     * @param timeout value of timeout option.
     * @return {@link UpdateOptions.Builder} object.
     * @throws IllegalArgumentException when timeout &#8804; 0.
     * @see UpdateOptions#timeout
     * @see UpdateOptions
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
     * @return {@link UpdateOptions.Builder} object
     * @throws IllegalArgumentException when streamId &#60; 0
     * @see UpdateOptions#streamId
     * @see UpdateOptions
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
     * @return {@link UpdateOptions.Builder} object
     * @see #TIMEOUT
     * @see UpdateOptions
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
     * @see UpdateOptions
     * @see UpdateOptions.Builder
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
     * @return {@link UpdateOptions.Builder} object
     * @see #BUCKET_ID
     * @see UpdateOptions
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
     * @return {@link UpdateOptions.Builder} object
     * @see #FIELDS
     * @see UpdateOptions
     */
    public Builder withFields(String... fields) {
      addOption(FIELDS, Arrays.asList(fields));
      return this;
    }

    /**
     * Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.
     *
     * @param fields value of {@link #FIELDS} option
     * @return {@link UpdateOptions.Builder} object
     * @see #FIELDS
     * @see UpdateOptions
     */
    public Builder withFields(List<String> fields) {
      addOption(FIELDS, fields);
      return this;
    }

    /**
     * Sets value of {@link #NO_RETURN} to <tt>true</tt>.
     *
     * @return {@link UpdateOptions.Builder} object
     * @see #NO_RETURN
     * @see UpdateOptions
     */
    public Builder withNoReturn() {
      addOption(NO_RETURN, true);
      return this;
    }

    /**
     * Sets value of {@link #VSHARD_ROUTER} option.
     *
     * @param vshardRouter value of {@link #VSHARD_ROUTER} option
     * @return {@link UpdateOptions.Builder}
     * @see #VSHARD_ROUTER
     * @see UpdateOptions
     */
    public Builder withVshardRouter(String vshardRouter) {
      addOption(VSHARD_ROUTER, vshardRouter);
      return this;
    }

    /**
     * Sets value of {@link #FETCH_LATEST_METADATA} to <tt>true</tt>.
     *
     * @return {@link UpdateOptions.Builder} object
     * @see #FETCH_LATEST_METADATA
     * @see UpdateOptions
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
     * @return {@link UpdateOptions.Builder}
     * @see UpdateOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * Builds object of {@link UpdateOptions} class.
     *
     * @return {@link UpdateOptions} object
     * @see UpdateOptions
     */
    public UpdateOptions build() {
      return new UpdateOptions(timeout, streamId, options);
    }
  }
}

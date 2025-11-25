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
 * href="https://github.com/tarantool/crud#min-and-max">min-and-max</a>.
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
 *   <li>{@value VSHARD_ROUTER}.
 *   <li>{@value FIELDS}.
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
 *  }};
 *
 *  MinMaxOptions option = new MinMaxOptions(timeout, null, options);
 *
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  // Inserts tuple (id = 2, isMarried = true, name = "Petya")
 *  space.insert(Arrays.asList(1, true, "Vanya")).get();
 *  space.insert(Arrays.asList(2, true, "Petya")).get();
 *
 *  // Returns tuple (id = 1, isMarried = true, name = "Vanya")
 *  List<?> res = space.min("pk", option).get();
 *
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
 *  MinMaxOptions option = MinMaxOptions.builder()
 *                                          .withTimeout(3_000L)
 *                                          .withCrudTimeout(2_000L)
 *                                          .build();
 *
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  // Inserts tuple (id = 2, isMarried = true, name = "Petya")
 *  space.insert(Arrays.asList(1, true, "Vanya")).get();
 *  space.insert(Arrays.asList(2, true, "Petya")).get();
 *
 *  // Returns tuple (id = 2, isMarried = true, name = "Petya")
 *  List<?> res = space.max("pk", option).get();
 *
 * }</pre>
 *
 * </blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudClient
 * @see TarantoolCrudSpace
 */
public class MinMaxOptions implements CrudOptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(MinMaxOptions.class);

  /**
   * {@link TarantoolCrudClient#call(String) vshard.call} timeout and vshard master discovery
   * timeout (in milliseconds).
   *
   * <p><i><b>Note</b></i>: The time indicated by this parameter is the time between sending a
   * message from the router to Tarantool instance and the time when the answer will come from
   * Tarantool instance to router.
   */
  private static final String TIMEOUT = "timeout";

  /** Field names for getting only a subset of fields. */
  private static final String FIELDS = "fields";

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
   * Creates a {@link MinMaxOptions} object with the given parameters.
   *
   * @param timeout {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options {@link #crudOptions}
   * @see MinMaxOptions
   */
  public MinMaxOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.crudOptions = options;

    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * Creates new builder instance of this class.
   *
   * @return {@link MinMaxOptions.Builder} object
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
   * Builder class for {@link MinMaxOptions}.
   *
   * @see MinMaxOptions
   */
  public static class Builder {

    /** See also: {@link MinMaxOptions#crudOptions}. */
    private final Map<String, Object> options = new HashMap<>();

    /** See also: {@link MinMaxOptions#timeout}. */
    private long timeout = DEFAULT_TIMEOUT;

    /** See also: {@link MinMaxOptions#streamId}. */
    private Long streamId;

    /**
     * Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.
     *
     * @param timeout value of timeout option.
     * @return {@link MinMaxOptions.Builder} object.
     * @throws IllegalArgumentException when timeout &#8804; 0.
     * @see MinMaxOptions#timeout
     * @see MinMaxOptions
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
     * @return {@link MinMaxOptions.Builder} object
     * @throws IllegalArgumentException when streamId &#60; 0
     * @see MinMaxOptions#streamId
     * @see MinMaxOptions
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
     * @return {@link MinMaxOptions.Builder} object
     * @see #TIMEOUT
     * @see MinMaxOptions
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
     * @see MinMaxOptions
     * @see MinMaxOptions.Builder
     */
    private void addOption(String name, Object value) {
      if (name == null) {
        LOGGER.warn("Option isn't used since name of option is null, value = {}", value);
        return;
      }
      this.options.put(name, value);
    }

    /**
     * Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.
     *
     * @param fields value of {@link #FIELDS} option
     * @return {@link MinMaxOptions.Builder} object
     * @see #FIELDS
     * @see MinMaxOptions
     */
    public Builder withFields(String... fields) {
      addOption(FIELDS, Arrays.asList(fields));
      return this;
    }

    /**
     * Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.
     *
     * @param fields value of {@link #FIELDS} option
     * @return {@link MinMaxOptions.Builder} object
     * @see #FIELDS
     * @see MinMaxOptions
     */
    public Builder withFields(List<String> fields) {
      addOption(FIELDS, fields);
      return this;
    }

    /**
     * Sets value of {@link #VSHARD_ROUTER} option.
     *
     * @param vshardRouter value of {@link #VSHARD_ROUTER} option
     * @return {@link MinMaxOptions.Builder}
     * @see #VSHARD_ROUTER
     * @see MinMaxOptions
     */
    public Builder withVshardRouter(String vshardRouter) {
      addOption(VSHARD_ROUTER, vshardRouter);
      return this;
    }

    /**
     * Sets options by name and value. OptionName parameter should not be equal {@code null}.
     *
     * @param optionName name of option
     * @param optionValue value of option
     * @return {@link MinMaxOptions.Builder}
     * @see MinMaxOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * Builds object of {@link MinMaxOptions} class.
     *
     * @return {@link MinMaxOptions} object
     * @see MinMaxOptions
     */
    public MinMaxOptions build() {
      return new MinMaxOptions(timeout, streamId, options);
    }
  }
}

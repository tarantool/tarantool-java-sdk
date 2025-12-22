/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.crud.options;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;

/**
 * <p> The class implements options for the CRUD operation
 * <a href="https://github.com/tarantool/crud#len"> len()</a>.</p>
 * <p>The following options are available:</p>
 * <p>Common options:</p>
 * <ul>
 *     <li>{@link #timeout}.</li>
 *     <li>{@link #streamId}.</li>
 * </ul>
 * <p>Crud options:</p>
 * <ul>
 *     <li>{@value TIMEOUT}.</li>
 *     <li>{@value VSHARD_ROUTER}.</li>
 * </ul>
 * <p>Examples:</p>
 * <blockquote><pre>{@code
 * <<Example 1>>
 *
 *  TarantoolCrudSpace space = crudClient.space("spaceName");
 *  long timeout = 3_000L;
 *  Map<String, Object> options = new HashMap<String, Object>(){{
 *      put("timeout", 2_000L);
 *  }};
 *
 *  LenOptions option = new LenOptions(timeout, null, options);
 *
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  space.insert(Arrays.asList(1, true, "Vanya")).get();
 *
 *  // Returns 1
 *  int len = space.len(option).get();
 *
 * }</pre></blockquote>
 * <blockquote><pre>{@code
 * <<Example 2>>
 *
 * TarantoolCrudSpace space = crudClient.space("spaceName");
 *
 * LenOptions options = LenOptions.builder()
 *                                      .withTimeout(3_000L)
 *                                      .withCrudTimeout(2_000L)
 *                                      .build();
 *
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  space.insert(Arrays.asList(1, true, "Vanya")).get();
 *
 *  // Returns 1
 *  int len = space.len(options).get();
 * }</pre></blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudClient
 * @see TarantoolCrudSpace
 */
public class LenOptions implements CrudOptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(LenOptions.class);

  /**
   * <p>{@link TarantoolCrudClient#call(String) vshard.call} timeout and vshard master
   * discovery timeout (in milliseconds).</p>
   * <p><i><b>Note</b></i>: The time indicated by this parameter is the time between sending a message from the
   * router to Tarantool instance and the time when the answer will come from Tarantool instance to router.</p>
   */
  private static final String TIMEOUT = "timeout";

  /**
   * <p>Cartridge vshard group name. Set this parameter if your space is not a part of
   * the default vshard cluster.</p>
   */
  private static final String VSHARD_ROUTER = "vshard_router";

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
   * <p>Creates a {@link LenOptions} object with the given parameters.</p>
   *
   * @param timeout  {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options  {@link #crudOptions}
   * @see LenOptions
   */
  public LenOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.crudOptions = options;

    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * <p>Creates new builder instance of this class.</p>
   *
   * @return {@link LenOptions.Builder} object
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
   * <p>Builder class for {@link LenOptions}.</p>
   *
   * @see LenOptions
   */
  public static class Builder {

    /**
     * <p>See also: {@link LenOptions#crudOptions}.</p>
     */
    private final Map<String, Object> options = new HashMap<>();

    /**
     * <p>See also: {@link LenOptions#timeout}.</p>
     */
    private long timeout = DEFAULT_TIMEOUT;

    /**
     * <p>See also: {@link LenOptions#streamId}.</p>
     */
    private Long streamId;

    /**
     * <p>Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.</p>
     *
     * @param timeout value of timeout option.
     * @return {@link LenOptions.Builder} object.
     * @throws IllegalArgumentException when timeout &#8804; 0.
     * @see LenOptions#timeout
     * @see LenOptions
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
     * @return {@link LenOptions.Builder} object
     * @throws IllegalArgumentException when streamId &#60; 0
     * @see LenOptions#streamId
     * @see LenOptions
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
     * @return {@link LenOptions.Builder} object
     * @see #TIMEOUT
     * @see LenOptions
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
     * @see LenOptions
     * @see LenOptions.Builder
     */
    private void addOption(String name, Object value) {
      if (name == null) {
        LOGGER.warn("Option isn't used since name of option is null, value = {}", value);
        return;
      }
      this.options.put(name, value);
    }

    /**
     * <p>Sets value of {@link #VSHARD_ROUTER} option.</p>
     *
     * @param vshardRouter value of {@link  #VSHARD_ROUTER} option
     * @return {@link LenOptions.Builder}
     * @see #VSHARD_ROUTER
     * @see LenOptions
     */
    public Builder withVshardRouter(String vshardRouter) {
      addOption(VSHARD_ROUTER, vshardRouter);
      return this;
    }

    /**
     * <p>Sets options by name and value. OptionName parameter should not be equal {@code null}.</p>
     *
     * @param optionName  name of option
     * @param optionValue value of option
     * @return {@link LenOptions.Builder}
     * @see LenOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * <p>Builds object of {@link LenOptions} class.</p>
     *
     * @return {@link LenOptions} object
     * @see LenOptions
     */
    public LenOptions build() {
      return new LenOptions(timeout, streamId, options);
    }
  }
}

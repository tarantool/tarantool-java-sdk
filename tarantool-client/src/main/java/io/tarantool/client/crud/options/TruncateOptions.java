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
 * <a href="https://github.com/tarantool/crud#truncate"> truncate()</a>.</p>
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
 *  TruncateOptions options = new TruncateOptions(timeout, null, options);
 *
 *  boolean isTruncated = space.truncate(options).get();
 * }</pre></blockquote>
 * <blockquote><pre>{@code
 * <<Example 2>>
 *
 *  TarantoolCrudSpace space = crudClient.space("spaceName");
 *
 *  CountOptions options = CountOptions.builder()
 *                                          .withTimeout(3_000L)
 *                                          .withStreamId(null)
 *                                          .withCrudTimeout(2_000L)
 *                                          .build();
 *
 *  boolean isTruncated = space.truncate(options).get();
 * }</pre></blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudSpace
 * @see TarantoolCrudClient
 */
public class TruncateOptions implements CrudOptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(TruncateOptions.class);

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
   * <p> Stream id for truncate operation.</p>
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
   * <p>Creates a {@link TruncateOptions} object with the given parameters.</p>
   *
   * @param timeout  {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options  {@link #crudOptions}
   * @see TruncateOptions
   */
  public TruncateOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.crudOptions = options;

    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * <p>Creates new builder instance of this class.</p>
   *
   * @return {@link TruncateOptions.Builder} object
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
   * <p>Returns an immutable option map for the truncate operation.</p>
   *
   * @return {@link Map} object.
   */
  @Override
  public Map<String, Object> getOptions() {
    return Collections.unmodifiableMap(crudOptions);
  }

  /**
   * <p>Builder class for {@link TruncateOptions}.</p>
   *
   * @see TruncateOptions
   */
  public static class Builder {

    /**
     * <p>See also: {@link TruncateOptions#crudOptions}.</p>
     */
    private final Map<String, Object> options = new HashMap<>();

    /**
     * <p>See also: {@link TruncateOptions#timeout}.</p>
     */
    private long timeout = DEFAULT_TIMEOUT;

    /**
     * <p>See also: {@link TruncateOptions#streamId}.</p>
     */
    private Long streamId;

    /**
     * <p>Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.</p>
     *
     * @param timeout value of timeout option.
     * @return {@link TruncateOptions.Builder} object.
     * @throws IllegalArgumentException when timeout &#8804; 0.
     * @see TruncateOptions#timeout
     * @see TruncateOptions
     */
    public Builder withTimeout(long timeout) throws IllegalArgumentException {
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
     * @return {@link TruncateOptions.Builder} object
     * @throws IllegalArgumentException when streamId &#60; 0
     * @see TruncateOptions#streamId
     * @see TruncateOptions
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
     * @return {@link TruncateOptions.Builder} object
     * @see #TIMEOUT
     * @see TruncateOptions
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
     * @see TruncateOptions
     * @see TruncateOptions.Builder
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
     * @return {@link TruncateOptions.Builder}
     * @see #VSHARD_ROUTER
     * @see TruncateOptions
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
     * @return {@link TruncateOptions.Builder}
     * @see TruncateOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * <p>Builds object of {@link TruncateOptions} class.</p>
     *
     * @return {@link TruncateOptions} object
     * @see TruncateOptions
     */
    public TruncateOptions build() {
      return new TruncateOptions(timeout, streamId, options);
    }
  }
}

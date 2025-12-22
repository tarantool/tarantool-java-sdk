/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Rename this class to a more elegant name.
 */
public class OnlyKeyValueOptions implements OptionsMap {

  private static final Logger LOGGER = LoggerFactory.getLogger(OnlyKeyValueOptions.class);
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
  private final Map<String, Object> options;

  /**
   * <p>Creates a {@link OnlyKeyValueOptions} object with the given parameters.</p>
   *
   * @param timeout  {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options  {@link #options}
   * @see OnlyKeyValueOptions
   */
  public OnlyKeyValueOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.options = options;

    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * <p>Creates new builder instance of this class.</p>
   *
   * @return {@link Builder} object
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
    return Collections.unmodifiableMap(options);
  }

  /**
   * <p>Builder class for {@link OnlyKeyValueOptions}.</p>
   *
   * @see OnlyKeyValueOptions
   */
  public static class Builder {

    /**
     * <p>See also: {@link OnlyKeyValueOptions#options}.</p>
     */
    private final Map<String, Object> options = new HashMap<>();

    /**
     * <p>See also: {@link OnlyKeyValueOptions#timeout}.</p>
     */
    private long timeout = DEFAULT_TIMEOUT;

    /**
     * <p>See also: {@link OnlyKeyValueOptions#streamId}.</p>
     */
    private Long streamId;

    /**
     * <p>Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.</p>
     *
     * @param timeout value of timeout option.
     * @return {@link Builder} object.
     * @throws IllegalArgumentException when timeout &#8804; 0.
     * @see OnlyKeyValueOptions#timeout
     * @see OnlyKeyValueOptions
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
     * @return {@link Builder} object
     * @throws IllegalArgumentException when streamId &#60; 0
     * @see OnlyKeyValueOptions#streamId
     * @see OnlyKeyValueOptions
     */
    public Builder withStreamId(Long streamId) {
      if (streamId < 0) {
        throw new IllegalArgumentException("streamId should be greater or equal 0");
      }
      this.streamId = streamId;
      return this;
    }

    /**
     * <p>Adds options by name into {@link #options} map. Name parameter should not be equal {@code null}.</p>
     *
     * @param name  name of option
     * @param value value of option
     * @see OnlyKeyValueOptions
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
     * <p>Sets options by name and value. OptionName parameter should not be equal {@code null}.</p>
     *
     * @param optionName  name of option
     * @param optionValue value of option
     * @return {@link Builder}
     * @see OnlyKeyValueOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * <p>Builds object of {@link OnlyKeyValueOptions} class.</p>
     *
     * @return {@link OnlyKeyValueOptions} object
     * @see OnlyKeyValueOptions
     */
    public OnlyKeyValueOptions build() {
      return new OnlyKeyValueOptions(timeout, streamId, options);
    }
  }
}

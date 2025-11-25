/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** TODO: Rename this class to a more elegant name. */
public class OnlyKeyValueOptions implements OptionsMap {

  private static final Logger LOGGER = LoggerFactory.getLogger(OnlyKeyValueOptions.class);

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
  private final Map<String, Object> options;

  /**
   * Creates a {@link OnlyKeyValueOptions} object with the given parameters.
   *
   * @param timeout {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options {@link #options}
   * @see OnlyKeyValueOptions
   */
  public OnlyKeyValueOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.options = options;

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
    return Collections.unmodifiableMap(options);
  }

  /**
   * Builder class for {@link OnlyKeyValueOptions}.
   *
   * @see OnlyKeyValueOptions
   */
  public static class Builder {

    /** See also: {@link OnlyKeyValueOptions#options}. */
    private final Map<String, Object> options = new HashMap<>();

    /** See also: {@link OnlyKeyValueOptions#timeout}. */
    private long timeout = DEFAULT_TIMEOUT;

    /** See also: {@link OnlyKeyValueOptions#streamId}. */
    private Long streamId;

    /**
     * Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.
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
     * Sets value of {@link #streamId} option. StreamId parameter should be greater or equal 0.
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
     * Adds options by name into {@link #options} map. Name parameter should not be equal {@code
     * null}.
     *
     * @param name name of option
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
     * Sets options by name and value. OptionName parameter should not be equal {@code null}.
     *
     * @param optionName name of option
     * @param optionValue value of option
     * @return {@link Builder}
     * @see OnlyKeyValueOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * Builds object of {@link OnlyKeyValueOptions} class.
     *
     * @return {@link OnlyKeyValueOptions} object
     * @see OnlyKeyValueOptions
     */
    public OnlyKeyValueOptions build() {
      return new OnlyKeyValueOptions(timeout, streamId, options);
    }
  }
}

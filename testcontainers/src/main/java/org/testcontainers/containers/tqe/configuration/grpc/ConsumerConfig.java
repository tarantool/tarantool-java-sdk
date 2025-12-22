/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type ConsumerConfig struct {
	Enabled          bool                  `mapstructure:"enabled"`
	AccessMode       string                `mapstructure:"access_mode"`
	Tarantool        TarantoolClientConfig `mapstructure:"tarantool"`
	CursorSerializer string                `mapstructure:"cursor_serializer"`
	PollingTimeout   time.Duration         `mapstructure:"polling_timeout"`
	Cache            ConsumerCacheConfig   `mapstructure:"cache"`
	BufferSize       uint                  `mapstructure:"buffer_size"`
}

 */
public class ConsumerConfig {

  @JsonProperty("enabled")
  private final Boolean enabled;

  @JsonProperty("access_mode")
  private final String accessMode;

  @JsonProperty("tarantool")
  private final TarantoolClientConfig tarantool;

  @JsonProperty("cursor_serializer")
  private final String cursorSerializer;

  @JsonProperty("polling_timeout")
  private final String pollingTimeout;

  @JsonProperty("cache")
  private final ConsumerCacheConfig cache;

  @JsonProperty("buffer_size")
  private final Long bufferSize;

  @JsonCreator
  public ConsumerConfig(
      @JsonProperty("enabled") Boolean enabled,
      @JsonProperty("access_mode") String accessMode,
      @JsonProperty("tarantool") TarantoolClientConfig tarantool,
      @JsonProperty("cursor_serializer") String cursorSerializer,
      @JsonProperty("polling_timeout") String pollingTimeout,
      @JsonProperty("cache") ConsumerCacheConfig cache,
      @JsonProperty("buffer_size") Long bufferSize) {
    this.enabled = enabled;
    this.accessMode = accessMode;
    this.tarantool = tarantool;
    this.cursorSerializer = cursorSerializer;
    this.pollingTimeout = pollingTimeout;
    this.cache = cache;
    this.bufferSize = bufferSize;
  }

  public Optional<Boolean> getEnabled() {
    return Optional.ofNullable(this.enabled);
  }

  public Optional<String> getAccessMode() {
    return Optional.ofNullable(this.accessMode);
  }

  public Optional<TarantoolClientConfig> getTarantool() {
    return Optional.ofNullable(this.tarantool);
  }

  public Optional<String> getCursorSerializer() {
    return Optional.ofNullable(this.cursorSerializer);
  }

  public Optional<String> getPollingTimeout() {
    return Optional.ofNullable(this.pollingTimeout);
  }

  public Optional<ConsumerCacheConfig> getCache() {
    return Optional.ofNullable(this.cache);
  }

  public Optional<Long> getBufferSize() {
    return Optional.ofNullable(this.bufferSize);
  }
}

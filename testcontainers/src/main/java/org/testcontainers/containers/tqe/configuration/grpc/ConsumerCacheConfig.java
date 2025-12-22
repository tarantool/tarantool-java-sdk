/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type ConsumerCacheConfig struct {
	Enabled        bool          `mapstructure:"enabled"`
	DebugMode      bool          `mapstructure:"debug_mode"`
	BufferSize     int           `mapstructure:"buffer_size"`
	MaxBuffers     int           `mapstructure:"max_buffers"`
	FetchBatchSize int           `mapstructure:"fetch_batch_size"`
	CheckDelay     time.Duration `mapstructure:"check_delay"`
}
 */
public class ConsumerCacheConfig {

  @JsonProperty("enabled")
  private final Boolean enabled;

  @JsonProperty("debug_mode")
  private final Boolean debugMode;

  @JsonProperty("buffer_size")
  private final Integer bufferSize;

  @JsonProperty("max_buffers")
  private final Integer maxBuffers;

  @JsonProperty("fetch_batch_size")
  private final Integer fetchBatchSize;

  @JsonProperty("check_delay")
  private final String checkDelay;

  @JsonCreator
  public ConsumerCacheConfig(
      @JsonProperty("enabled") Boolean enabled,
      @JsonProperty("debug_mode") Boolean debugMode,
      @JsonProperty("buffer_size") Integer bufferSize,
      @JsonProperty("max_buffers") Integer maxBuffers,
      @JsonProperty("fetch_batch_size") Integer fetchBatchSize,
      @JsonProperty("check_delay") String checkDelay) {
    this.enabled = enabled;
    this.debugMode = debugMode;
    this.bufferSize = bufferSize;
    this.maxBuffers = maxBuffers;
    this.fetchBatchSize = fetchBatchSize;
    this.checkDelay = checkDelay;
  }

  public Optional<Boolean> getEnabled() {
    return Optional.ofNullable(enabled);
  }

  public Optional<Boolean> getDebugMode() {
    return Optional.ofNullable(debugMode);
  }

  public Optional<Integer> getBufferSize() {
    return Optional.ofNullable(bufferSize);
  }

  public Optional<Integer> getMaxBuffers() {
    return Optional.ofNullable(maxBuffers);
  }

  public Optional<Integer> getFetchBatchSize() {
    return Optional.ofNullable(fetchBatchSize);
  }

  public Optional<String> getCheckDelay() {
    return Optional.ofNullable(checkDelay);
  }
}

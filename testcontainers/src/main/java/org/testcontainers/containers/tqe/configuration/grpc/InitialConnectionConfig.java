/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type InitialConnectionConfig struct {
	RetryCount   uint          `mapstructure:"retry_count"`
	RetryTimeout time.Duration `mapstructure:"retry_timeout"`
	RetryDelay   time.Duration `mapstructure:"retry_delay"`
}
 */
public class InitialConnectionConfig {

  @JsonProperty("retry_count")
  private final Long retryCount;

  @JsonProperty("retry_timeout")
  private final String retryTimeout;

  @JsonProperty("retry_delay")
  private final String retryDelay;

  @JsonCreator
  public InitialConnectionConfig(
      @JsonProperty("retry_count") Long retryCount,
      @JsonProperty("retry_timeout") String retryTimeout,
      @JsonProperty("retry_delay") String retryDelay) {
    this.retryCount = retryCount;
    this.retryTimeout = retryTimeout;
    this.retryDelay = retryDelay;
  }

  public Optional<Long> getRetryCount() {
    return Optional.ofNullable(this.retryCount);
  }

  public Optional<String> getRetryTimeout() {
    return Optional.ofNullable(this.retryTimeout);
  }

  public Optional<String> getRetryDelay() {
    return Optional.ofNullable(this.retryDelay);
  }
}

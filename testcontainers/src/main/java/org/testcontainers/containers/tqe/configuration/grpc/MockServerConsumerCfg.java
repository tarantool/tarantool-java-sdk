/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type MockServerConsumerCfg struct {
	BatchSize uint `mapstructure:"batch_size"`
	SleepTime time.Duration `mapstructure:"sleep_time"`
}
 */
public class MockServerConsumerCfg {

  @JsonProperty("batch_size")
  private final Long batchSize;

  @JsonProperty("sleep_time")
  private final String sleepTime;

  @JsonCreator
  public MockServerConsumerCfg(
      @JsonProperty("batch_size") Long batchSize, @JsonProperty("sleep_time") String sleepTime) {
    this.batchSize = batchSize;
    this.sleepTime = sleepTime;
  }

  public Optional<Long> getBatchSize() {
    return Optional.ofNullable(batchSize);
  }

  public Optional<String> getSleepTime() {
    return Optional.ofNullable(sleepTime);
  }
}

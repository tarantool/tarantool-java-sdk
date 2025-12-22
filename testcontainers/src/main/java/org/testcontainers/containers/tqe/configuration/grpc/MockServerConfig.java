/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type MockServerConfig struct {
	Enabled bool `mapstructure:"enabled"`
	QueueType string `mapstructure:"queue_type"`
	EchoQueueCfg EchoQueueCfg `mapstructure:"echo_queue"`
	SliceQueueCfg SliceQueueCfg `mapstructure:"slice_queue"`
	ConsumerCfg MockServerConsumerCfg `mapstructure:"consumer"`
	SimpleGRPC bool `mapstructure:"simple_grpc"`
}
 */
public class MockServerConfig {

  @JsonProperty("enabled")
  private final Boolean enabled;

  @JsonProperty("queue_type")
  private final String queueType;

  @JsonProperty("echo_queue")
  private final EchoQueueCfg echoQueueCfg;

  @JsonProperty("slice_queue")
  private final SliceQueueCfg sliceQueueCfg;

  @JsonProperty("consumer")
  private final MockServerConsumerCfg consumerCfg;

  @JsonProperty("simple_grpc")
  private final Boolean simpleGrpc;

  @JsonCreator
  public MockServerConfig(
      @JsonProperty("enabled") Boolean enabled,
      @JsonProperty("queue_type") String queueType,
      @JsonProperty("echo_queue") EchoQueueCfg echoQueueCfg,
      @JsonProperty("slice_queue") SliceQueueCfg sliceQueueCfg,
      @JsonProperty("consumer") MockServerConsumerCfg consumerCfg,
      @JsonProperty("simple_grpc") Boolean simpleGrpc) {
    this.enabled = enabled;
    this.queueType = queueType;
    this.echoQueueCfg = echoQueueCfg;
    this.sliceQueueCfg = sliceQueueCfg;
    this.consumerCfg = consumerCfg;
    this.simpleGrpc = simpleGrpc;
  }

  public Optional<Boolean> getEnabled() {
    return Optional.ofNullable(enabled);
  }

  public Optional<String> getQueueType() {
    return Optional.ofNullable(queueType);
  }

  public Optional<EchoQueueCfg> getEchoQueueCfg() {
    return Optional.ofNullable(echoQueueCfg);
  }

  public Optional<SliceQueueCfg> getSliceQueueCfg() {
    return Optional.ofNullable(sliceQueueCfg);
  }

  public Optional<MockServerConsumerCfg> getConsumerCfg() {
    return Optional.ofNullable(consumerCfg);
  }

  public Optional<Boolean> getSimpleGrpc() {
    return Optional.ofNullable(simpleGrpc);
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type EchoQueueCfg struct {
	PayloadSize int `mapstructure:"payload_size"`
	QueueName string `mapstructure:"queue_name"`
}

 */
public class EchoQueueCfg {

  @JsonProperty("payload_size")
  private final Integer payloadSize;

  @JsonProperty("queue_name")
  private final String queueName;

  @JsonCreator
  public EchoQueueCfg(
      @JsonProperty("payload_size") Integer payloadSize,
      @JsonProperty("queue_name") String queueName) {
    this.payloadSize = payloadSize;
    this.queueName = queueName;
  }

  public Optional<Integer> getPayloadSize() {
    return Optional.ofNullable(payloadSize);
  }

  public Optional<String> getQueueName() {
    return Optional.ofNullable(queueName);
  }
}

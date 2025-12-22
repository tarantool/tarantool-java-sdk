/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type TarantoolClientQueueConfig struct {
	Connections map[string][]string `mapstructure:"connections"`
}
 */
public class TarantoolClientQueueConfig {

  @JsonProperty("connections")
  private final Map<String, Set<String>> connections;

  @JsonCreator
  public TarantoolClientQueueConfig(@JsonProperty("connections") Map<String, Set<String>> connections) {
    this.connections = connections;
  }

  public Optional<Map<String, Set<String>>> getConnections() {
    return Optional.ofNullable(this.connections);
  }
}

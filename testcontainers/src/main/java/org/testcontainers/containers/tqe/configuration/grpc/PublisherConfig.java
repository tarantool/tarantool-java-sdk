/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type PublisherConfig struct {
	Enabled      bool                  `mapstructure:"enabled"`
	LocalRouting bool                  `mapstructure:"local_routing"`
	Tarantool    TarantoolClientConfig `mapstructure:"tarantool"`
}
 */
public class PublisherConfig {

  @JsonProperty("enabled")
  private final Boolean enabled;

  @JsonProperty("local_routing")
  private final Boolean localRoutingEnabled;

  @JsonProperty("tarantool")
  private final TarantoolClientConfig tarantool;

  @JsonCreator
  public PublisherConfig(
      @JsonProperty("enabled") Boolean enabled,
      @JsonProperty("local_routing") Boolean localRoutingEnabled,
      @JsonProperty("tarantool") TarantoolClientConfig tarantool) {
    this.enabled = enabled;
    this.localRoutingEnabled = localRoutingEnabled;
    this.tarantool = tarantool;
  }

  public Optional<Boolean> getEnabled() {
    return Optional.ofNullable(this.enabled);
  }

  public Optional<Boolean> getLocalRoutingEnabled() {
    return Optional.ofNullable(this.localRoutingEnabled);
  }

  public Optional<TarantoolClientConfig> getTarantool() {
    return Optional.ofNullable(this.tarantool);
  }
}

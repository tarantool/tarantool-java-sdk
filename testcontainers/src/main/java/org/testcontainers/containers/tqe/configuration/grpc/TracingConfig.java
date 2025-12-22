/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type TracingConfig struct {
	JaegerCollectorEndpoint string `mapstructure:"jaeger_collector_endpoint"`
	Enabled                 bool   `mapstructure:"enabled"`
}
 */
public class TracingConfig {

  @JsonProperty("jaeger_collector_endpoint")
  private final String jaegerCollectorEndpoint;

  @JsonProperty("enabled")
  private final Boolean enabled;

  @JsonCreator
  public TracingConfig(
      @JsonProperty("jaeger_collector_endpoint") String jaegerCollectorEndpoint,
      @JsonProperty("enabled") Boolean enabled) {
    this.jaegerCollectorEndpoint = jaegerCollectorEndpoint;
    this.enabled = enabled;
  }

  public Optional<String> getJaegerCollectorEndpoint() {
    return Optional.ofNullable(jaegerCollectorEndpoint);
  }

  public Optional<Boolean> getEnabled() {
    return Optional.ofNullable(enabled);
  }
}

/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

/*
type GrpcListen struct {
  Uri string `mapstructure:"uri"`
}
 */

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GrpcListen {

  @JsonProperty("uri")
  private String grpcHost;

  @JsonCreator
  public GrpcListen(@JsonProperty("uri") String grpcHost) {
    this.grpcHost = grpcHost;
  }

  public Optional<String> getGrpcHost() {
    return Optional.ofNullable(this.grpcHost);
  }
}

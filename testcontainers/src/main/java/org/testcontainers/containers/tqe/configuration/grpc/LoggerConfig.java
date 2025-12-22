/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type LoggerConfig struct {
	Level       string `mapstructure:"level"`
	Format      string `mapstructure:"format"`
	File        string `mapstructure:"file"`
	NonBlocking bool   `mapstructure:"non_blocking"`
}
 */
public class LoggerConfig {

  @JsonProperty("level")
  private final String level;

  @JsonProperty("format")
  private final String format;

  @JsonProperty("file")
  private final String file;

  @JsonProperty("non_blocking")
  private final Boolean nonBlocking;

  @JsonCreator
  public LoggerConfig(
      @JsonProperty("level") String level,
      @JsonProperty("format") String format,
      @JsonProperty("file") String file,
      @JsonProperty("non_blocking") Boolean nonBlocking) {
    this.level = level;
    this.format = format;
    this.file = file;
    this.nonBlocking = nonBlocking;
  }

  public Optional<String> getLevel() {
    return Optional.ofNullable(level);
  }

  public Optional<String> getFormat() {
    return Optional.ofNullable(format);
  }

  public Optional<String> getFile() {
    return Optional.ofNullable(file);
  }

  public Optional<Boolean> getNonBlocking() {
    return Optional.ofNullable(nonBlocking);
  }
}

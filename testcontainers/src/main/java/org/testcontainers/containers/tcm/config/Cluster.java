/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tcm.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Cluster {

  @JsonProperty("tt-command")
  private final String ttCommand;

  @JsonCreator
  Cluster(@JsonProperty("tt-command") String ttCommand) {
    this.ttCommand = ttCommand;
  }

  public String getTtCommand() {
    return this.ttCommand;
  }
}

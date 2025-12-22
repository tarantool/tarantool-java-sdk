/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tcm.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Security {

  @JsonProperty("bootstrap-password")
  private final CharSequence bootstrapPassword;

  @JsonCreator
  Security(@JsonProperty("bootstrap-password") CharSequence bootstrapPassword) {
    this.bootstrapPassword = bootstrapPassword;
  }

  public CharSequence getBootstrapPassword() {
    return this.bootstrapPassword;
  }
}

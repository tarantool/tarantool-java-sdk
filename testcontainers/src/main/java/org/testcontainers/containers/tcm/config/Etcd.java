/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tcm.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Etcd {

  private final String prefix;

  private final String username;

  private final CharSequence password;

  private final List<String> endpoints;

  @JsonCreator
  Etcd(@JsonProperty("prefix") String prefix, @JsonProperty("username") String username,
      @JsonProperty("password") CharSequence password, @JsonProperty("endpoints") List<String> endpoints) {
    this.prefix = prefix;
    this.username = username;
    this.password = password;
    this.endpoints = endpoints;
  }

  public String getPrefix() {
    return this.prefix;
  }

  public String getUsername() {
    return this.username;
  }

  public CharSequence getPassword() {
    return this.password;
  }

  public List<String> getEndpoints() {
    return this.endpoints;
  }
}

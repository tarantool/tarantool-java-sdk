/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tcm.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Http {

  public static final String DEFAULT_TCM_HTTP_HOST = "0.0.0.0";

  public static final int DEFAULT_TCM_HTTP_PORT = 8080;

  private final String host;

  private final Integer port;

  @JsonCreator
  Http(@JsonProperty("host") String host, @JsonProperty("port") Integer port) {
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return this.host;
  }

  public Integer getPort() {
    return this.port;
  }

  public static Http defaultHttp() {
    return new Http(DEFAULT_TCM_HTTP_HOST, DEFAULT_TCM_HTTP_PORT);
  }
}

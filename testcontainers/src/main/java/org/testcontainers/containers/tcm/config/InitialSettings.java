/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tcm.config;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InitialSettings {

  @JsonProperty("clusters")
  private final ClusterConfig cluster;

  @JsonCreator
  InitialSettings(@JsonProperty("clusters") List<ClusterConfig> clusters) {
    this.cluster = clusters.get(0);
  }

  public List<ClusterConfig> getCluster() {
    return Collections.singletonList(cluster);
  }

  public ClusterConfig cluster() {
    return this.cluster;
  }
}

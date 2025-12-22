/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tcm.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Storage {

  private final String provider;

  private final Etcd etcd;

  @JsonCreator
  Storage(@JsonProperty("provider") String provider, @JsonProperty("etcd") Etcd etcd) {
    this.provider = provider;
    this.etcd = etcd;
  }

  public String getProvider() {
    return this.provider;
  }

  public Etcd getEtcd() {
    return this.etcd;
  }
}

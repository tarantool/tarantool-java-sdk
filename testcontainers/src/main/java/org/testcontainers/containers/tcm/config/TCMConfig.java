/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tcm.config;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.testcontainers.containers.tcm.config.ClusterConfig.StorageConnection;
import org.testcontainers.containers.tcm.config.ClusterConfig.TarantoolConnection;

public class TCMConfig {

  public static final String DEFAULT_TARANTOOL_USERNAME = "test-super";

  public static final CharSequence DEFAULT_TARANTOOL_PASSWORD = "test";

  public static final String DEFAULT_ETCD_ADDRESS = "http://etcd:2379";

  public static final String ETCD_NAME = "etcd";

  public static final String DEFAULT_TDB_PREFIX = "/tdb";

  private final Http http;

  private final Storage storage;

  private final Security security;

  private final Cluster cluster;

  @JsonProperty("initial-settings")
  private final InitialSettings initialSettings;

  @JsonCreator
  TCMConfig(
      @JsonProperty("http") Http http,
      @JsonProperty("storage") Storage storage,
      @JsonProperty("security") Security security,
      @JsonProperty("cluster") Cluster cluster,
      @JsonProperty("initial-settings") InitialSettings initialSettings) {
    this.http = http;
    this.storage = storage;
    this.security = security;
    this.cluster = cluster;
    this.initialSettings = initialSettings;
  }

  public Http getHttp() {
    return this.http;
  }

  public Storage getStorage() {
    return this.storage;
  }

  public Security getSecurity() {
    return this.security;
  }

  public Cluster getCluster() {
    return this.cluster;
  }

  public InitialSettings getInitialSettings() {
    return this.initialSettings;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String etcdAddress = DEFAULT_ETCD_ADDRESS;

    private String tarantoolUsername = DEFAULT_TARANTOOL_USERNAME;

    private CharSequence tarantoolPassword = DEFAULT_TARANTOOL_PASSWORD;

    public Builder withEtcdAddress(String address) {
      this.etcdAddress = address;
      return this;
    }

    public Builder withTarantoolUsername(String username) {
      this.tarantoolUsername = username;
      return this;
    }

    public Builder withTarantoolPassword(CharSequence password) {
      this.tarantoolPassword = password;
      return this;
    }

    public TCMConfig build() {
      final Http httpSettings = Http.defaultHttp();
      final List<String> etcdEndpoints = Collections.singletonList(this.etcdAddress);

      final Etcd storageEtcd = new Etcd("/tcm", null, null, etcdEndpoints);

      final Storage storage = new Storage(ETCD_NAME, storageEtcd);
      final Security security = new Security("secret");

      final Etcd storageConnectionEtcd = new Etcd(DEFAULT_TDB_PREFIX, null, null, etcdEndpoints);

      final StorageConnection storageConnection =
          new StorageConnection(ETCD_NAME, storageConnectionEtcd);

      final TarantoolConnection tarantoolConnection =
          new TarantoolConnection(this.tarantoolUsername, this.tarantoolPassword);

      final ClusterConfig clusterConfig =
          new ClusterConfig(
              "TDB test cluster",
              UUID.fromString("00000000-0000-0000-0000-000000000000"),
              storageConnection,
              tarantoolConnection);

      final InitialSettings initialSettings =
          new InitialSettings(Collections.singletonList(clusterConfig));
      final Cluster cluster = new Cluster("/app/tarantooldb/tt");

      return new TCMConfig(httpSettings, storage, security, cluster, initialSettings);
    }
  }
}

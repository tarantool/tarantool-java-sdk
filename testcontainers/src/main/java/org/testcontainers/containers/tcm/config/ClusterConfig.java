/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tcm.config;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterConfig {

  private final String name;

  private final UUID id;

  @JsonProperty("storage-connection")
  private final StorageConnection storageConnection;

  @JsonProperty("tarantool-connection")
  private final TarantoolConnection tarantoolConnection;

  @JsonCreator
  ClusterConfig(
      @JsonProperty("name") String name,
      @JsonProperty("id") UUID id,
      @JsonProperty("storage-connection") StorageConnection storageConnection,
      @JsonProperty("tarantool-connection") TarantoolConnection tarantoolConnection) {
    this.name = name;
    this.id = id;
    this.storageConnection = storageConnection;
    this.tarantoolConnection = tarantoolConnection;
  }

  public String getName() {
    return this.name;
  }

  public UUID getId() {
    return this.id;
  }

  public StorageConnection getStorageConnection() {
    return this.storageConnection;
  }

  public TarantoolConnection getTarantoolConnection() {
    return this.tarantoolConnection;
  }

  public static class StorageConnection {

    private final String provider;

    @JsonProperty("etcd-connection")
    private final Etcd etcdConnection;

    @JsonCreator
    public StorageConnection(
        @JsonProperty("provider") String provider,
        @JsonProperty("etcd-connection") Etcd etcdConnection) {
      this.provider = provider;
      this.etcdConnection = etcdConnection;
    }

    public String getProvider() {
      return provider;
    }

    public Etcd getEtcdConnection() {
      return etcdConnection;
    }
  }

  public static class TarantoolConnection {

    private final String username;

    private final CharSequence password;

    @JsonCreator
    public TarantoolConnection(
        @JsonProperty("username") String username,
        @JsonProperty("password") CharSequence password) {
      this.username = username;
      this.password = password;
    }

    public String getUsername() {
      return this.username;
    }

    public CharSequence getPassword() {
      return this.password;
    }
  }
}

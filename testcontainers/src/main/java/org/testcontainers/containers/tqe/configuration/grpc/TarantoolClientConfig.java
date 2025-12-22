/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type TarantoolClientConfig struct {
	User string `mapstructure:"user"`
	Pass string `mapstructure:"pass"`

	Queues            map[string]TarantoolClientQueueConfig `mapstructure:"queues"`
	Connections       map[string][]string                   `mapstructure:"connections"`
	Replicasets       []string                              `mapstructure:"replicasets"`
	InitialConnection InitialConnectionConfig               `mapstructure:"initial_connection"`
}
 */
public class TarantoolClientConfig {

  @JsonProperty("user")
  private final String user;

  @JsonProperty("pass")
  private final String pass;

  @JsonProperty("queues")
  private final Map<String, TarantoolClientQueueConfig> queues;

  @JsonProperty("connections")
  private final Map<String, Set<String>> connections;

  @JsonProperty("replicasets")
  private final Set<String> replicasets;

  @JsonProperty("initial_connection")
  private final InitialConnectionConfig initialConnection;

  @JsonCreator
  public TarantoolClientConfig(
      @JsonProperty("user") String user,
      @JsonProperty("pass") String pass,
      @JsonProperty("queues") Map<String, TarantoolClientQueueConfig> queues,
      @JsonProperty("connections") Map<String, Set<String>> connections,
      @JsonProperty("replicasets") Set<String> replicasets,
      @JsonProperty("initial_connection") InitialConnectionConfig initialConnection) {
    this.user = user;
    this.pass = pass;
    this.queues = queues;
    this.connections = connections;
    this.replicasets = replicasets;
    this.initialConnection = initialConnection;
  }

  public Optional<String> getUser() {
    return Optional.ofNullable(this.user);
  }

  public Optional<String> getPass() {
    return Optional.ofNullable(this.pass);
  }

  public Optional<Map<String, TarantoolClientQueueConfig>> getQueues() {
    return Optional.ofNullable(this.queues);
  }

  public Optional<Map<String, Set<String>>> getConnections() {
    return Optional.ofNullable(this.connections);
  }

  public Optional<Set<String>> getReplicasets() {
    return Optional.ofNullable(this.replicasets);
  }

  public Optional<InitialConnectionConfig> getInitialConnection() {
    return Optional.ofNullable(this.initialConnection);
  }
}

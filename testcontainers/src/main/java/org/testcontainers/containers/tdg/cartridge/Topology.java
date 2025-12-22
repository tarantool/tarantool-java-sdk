/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdg.cartridge;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Topology {

  @JsonProperty(required = true)
  private final Map<UUID, Replicaset> replicasets;

  @JsonProperty(required = true)
  private final Map<UUID, Server> servers;

  @JsonCreator
  public Topology(
      @JsonProperty("replicasets") Map<UUID, Replicaset> replicasets,
      @JsonProperty("servers") Map<UUID, Server> servers) {
    this.replicasets = replicasets;
    this.servers = servers;
  }

  public Map<UUID, Replicaset> getReplicasets() {
    return this.replicasets;
  }

  public Map<UUID, Server> getServers() {
    return this.servers;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Replicaset {

    @JsonProperty(required = true)
    private final List<UUID> master;

    @JsonProperty(required = true)
    private final String alias;

    @JsonProperty(required = true)
    private final Map<String, Boolean> roles;

    @JsonCreator
    public Replicaset(
        @JsonProperty("master") List<UUID> master,
        @JsonProperty("alias") String alias,
        @JsonProperty("roles") Map<String, Boolean> roles) {
      this.master = master;
      this.alias = alias;
      this.roles = roles;
    }

    public List<UUID> getMaster() {
      return this.master;
    }

    public String getAlias() {
      return this.alias;
    }

    public Map<String, Boolean> getRoles() {
      return this.roles;
    }

    /**
     * @param enabled sets marker that indicates what roles (enabled or disabled) need to get
     */
    public Set<String> getRoleList(boolean enabled) {
      return getRoles().entrySet().stream()
          .filter(e -> enabled == e.getValue())
          .map(Entry::getKey)
          .collect(Collectors.toSet());
    }

    /**
     * Returns value that indicates whether the role is present in the replica set. If the role is
     * marked as disabled, method returns false.
     *
     * @param role finding role
     */
    public boolean hasRole(String role) {
      final Boolean enabled = getRoles().get(role);
      if (enabled == null) {
        return false;
      }
      return enabled;
    }
  }

  public static class Server {

    @JsonProperty(required = true)
    private final String uri;

    private final Boolean disabled;

    @JsonProperty(value = "replicaset_uuid", required = true)
    private final UUID replicasetUuid;

    @JsonCreator
    public Server(
        @JsonProperty("uri") String uri,
        @JsonProperty("disabled") Boolean disabled,
        @JsonProperty("replicaset_uuid") UUID replicasetUuid) {
      this.uri = uri;
      this.disabled = disabled;
      this.replicasetUuid = replicasetUuid;
    }

    public String getUri() {
      return uri;
    }

    public Boolean getDisabled() {
      return disabled;
    }

    public UUID getReplicasetUuid() {
      return replicasetUuid;
    }
  }
}

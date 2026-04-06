/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.testcontainers.containers.tarantool.config.ConfigurationUtils;
import org.yaml.snakeyaml.Yaml;

import io.tarantool.autogen.Tarantool3Configuration;
import io.tarantool.autogen.groups.groupsProperty.GroupsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.ReplicasetsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.InstancesProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.Iproto;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.listen.Listen;

public class TarantoolConfigParser {

  private static final String ROUTERS_GROUP_NAME = "routers";

  private final Tarantool3Configuration config;
  private final Config fallbackConfig;

  public TarantoolConfigParser(String instanceFileName) {
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(instanceFileName);
    if (inputStream == null) {
      throw new IllegalArgumentException(
          String.format("No resource path found for the specified resource %s", instanceFileName));
    }
    Tarantool3Configuration parsedConfig = null;
    Config parsedFallbackConfig = null;
    try (InputStream is = inputStream) {
      String rawConfig = readAsString(is);
      try {
        parsedConfig = ConfigurationUtils.create(rawConfig);
      } catch (LinkageError e) {
        parsedFallbackConfig = new Yaml().loadAs(rawConfig, Config.class);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.config = parsedConfig;
    this.fallbackConfig = parsedFallbackConfig;
  }

  public Integer[] getExposablePorts() {
    if (config != null) {
      return getExposablePortsFromTypedConfig();
    }
    return getExposablePortsFromFallbackConfig();
  }

  private Integer[] getExposablePortsFromTypedConfig() {
    if (config.getGroups().isEmpty()) {
      return new Integer[] {};
    }
    GroupsProperty routersGroup =
        config.getGroups().get().getAdditionalProperties().get(ROUTERS_GROUP_NAME);
    if (routersGroup == null || !routersGroup.getReplicasets().isPresent()) {
      return new Integer[] {};
    }
    List<Integer> ports =
        routersGroup.getReplicasets().get().getAdditionalProperties().values().stream()
            .map(ReplicasetsProperty::getInstances)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(instances -> instances.getAdditionalProperties().values())
            .flatMap(Collection::stream)
            .map(InstancesProperty::getIproto)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Iproto::getListen)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(List::stream)
            .map(Listen::getUri)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(TarantoolConfigParser::parsePort)
            .toList();

    return ports.toArray(new Integer[] {});
  }

  private Integer[] getExposablePortsFromFallbackConfig() {
    if (fallbackConfig == null
        || fallbackConfig.getGroups() == null
        || !fallbackConfig.getGroups().containsKey(ROUTERS_GROUP_NAME)) {
      return new Integer[] {};
    }
    Group routersGroup = fallbackConfig.getGroups().get(ROUTERS_GROUP_NAME);
    if (routersGroup == null || routersGroup.getReplicasets() == null) {
      return new Integer[] {};
    }
    List<Integer> ports =
        routersGroup.getReplicasets().values().stream()
            .map(Replicaset::getInstances)
            .filter(Objects::nonNull)
            .map(Map::values)
            .flatMap(Collection::stream)
            .map(Instance::getIproto)
            .filter(Objects::nonNull)
            .map(Map::values)
            .flatMap(Collection::stream)
            .flatMap(Collection::stream)
            .map(Map::values)
            .flatMap(Collection::stream)
            .map(TarantoolConfigParser::parsePort)
            .toList();
    return ports.toArray(new Integer[] {});
  }

  private static Integer parsePort(String uri) {
    if (uri == null || uri.isBlank()) {
      throw new IllegalArgumentException("Listen URI must not be null or empty");
    }
    String normalized = uri.trim();
    String uriWithScheme = normalized.contains("://") ? normalized : "tcp://" + normalized;
    try {
      URI parsed = URI.create(uriWithScheme);
      int port = parsed.getPort();
      if (port < 0) {
        throw new IllegalArgumentException("Listen URI must contain a valid port: " + uri);
      }
      return port;
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Failed to parse listen URI: " + uri, e);
    }
  }

  private static String readAsString(InputStream inputStream) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    return outputStream.toString(StandardCharsets.UTF_8);
  }
}

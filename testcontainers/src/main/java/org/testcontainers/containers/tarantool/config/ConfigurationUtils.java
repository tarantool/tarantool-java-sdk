/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.testcontainers.containers.tdb.TDB2ClusterImpl.DEFAULT_IPROTO_TARANTOOL_PORT;
import static org.testcontainers.containers.tdb.TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER;
import static org.testcontainers.containers.tdb.TDB2ClusterImpl.DEFAULT_TEST_SUPER_USER_PWD;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.tarantool.config.tarantool3.HostPort;
import org.testcontainers.containers.tqe.configuration.grpc.GrpcConfiguration;
import org.testcontainers.containers.utils.Utils;

import io.tarantool.autogen.Tarantool3Configuration;
import io.tarantool.autogen.credentials.Credentials;
import io.tarantool.autogen.credentials.users.Users;
import io.tarantool.autogen.credentials.users.usersProperty.UsersProperty;
import io.tarantool.autogen.groups.Groups;
import io.tarantool.autogen.groups.groupsProperty.GroupsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.Replicasets;
import io.tarantool.autogen.groups.groupsProperty.replicasets.Replicasets.ReplicasetsBuilderBase;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.ReplicasetsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.ReplicasetsProperty.ReplicasetsPropertyBuilderBase;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.Instances;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.Instances.InstancesBuilderBase;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.InstancesProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.Iproto;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.advertise.Advertise;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.listen.Listen;
import io.tarantool.autogen.groups.groupsProperty.replication.Replication;
import io.tarantool.autogen.groups.groupsProperty.replication.Replication.Failover;
import io.tarantool.autogen.groups.groupsProperty.sharding.Role;
import io.tarantool.autogen.groups.groupsProperty.sharding.Sharding;

public class ConfigurationUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationUtils.class);

  private static final YAMLMapper mapper;

  static {
    mapper = new YAMLMapper();
    mapper
        .enable(SerializationFeature.INDENT_OUTPUT)
        .setSerializationInclusion(Include.NON_NULL)
        .setSerializationInclusion(Include.NON_EMPTY);
    mapper.registerModule(new Jdk8Module());
  }

  private ConfigurationUtils() {}

  /**
   * Creates {@link Tarantool3Configuration} instance from raw string configuration representation
   */
  public static Tarantool3Configuration create(String configuration) {
    try {
      return mapper.readValue(configuration, Tarantool3Configuration.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /** Converts {@link Tarantool3Configuration} instance into raw string representation */
  public static String writeAsString(Tarantool3Configuration configuration) {
    try {
      return mapper.writeValueAsString(configuration);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static Tarantool3Configuration readFromFile(Path path) {
    try {
      return mapper.readValue(path.toFile(), Tarantool3Configuration.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeToFile(Tarantool3Configuration configuration, Path path) {
    try {
      mapper.writeValue(path.toFile(), configuration);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Reads {@link GrpcConfiguration} instance from configuration file. */
  public static GrpcConfiguration readGrpcFromFile(Path configPath) {
    try {
      return mapper.readValue(configPath.toFile(), GrpcConfiguration.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Reads configuration from file in string representation */
  public static String readRawFromFile(Path path) {
    try {
      return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates {@link Tarantool3Configuration} instance with simple cluster configuration with passed
   * router, shard, and replica counts
   *
   * @param routerCount count of routers
   * @param shardCount count of shards
   * @param replicaCount count of replicas in one shard
   */
  @SuppressWarnings("unchecked")
  public static Tarantool3Configuration generateSimpleConfiguration(
      int routerCount, int shardCount, int replicaCount) {
    final Credentials credentials =
        Credentials.builder()
            .withUsers(
                Users.builder()
                    .withAdditionalProperty(
                        DEFAULT_TEST_SUPER_USER,
                        UsersProperty.builder()
                            .withPassword(DEFAULT_TEST_SUPER_USER_PWD.toString())
                            .withRoles(Collections.singletonList("super"))
                            .build())
                    .withAdditionalProperty(
                        "storage",
                        UsersProperty.builder()
                            .withPassword("secret")
                            .withRoles(Collections.singletonList("sharding"))
                            .build())
                    .withAdditionalProperty(
                        "replicator",
                        UsersProperty.builder()
                            .withPassword("secret")
                            .withRoles(Collections.singletonList("replication"))
                            .build())
                    .build())
            .build();

    final ReplicasetsBuilderBase<?> routerReplicasetsBuilder = Replicasets.builder();
    for (int i = 1; i <= routerCount; i++) {
      final String routerName = "router-" + i;
      final String uri = routerName + ":" + DEFAULT_IPROTO_TARANTOOL_PORT;
      final String localhostUri = "localhost:" + DEFAULT_IPROTO_TARANTOOL_PORT;
      routerReplicasetsBuilder.withAdditionalProperty(
          routerName,
          ReplicasetsProperty.builder()
              .withLeader(routerName)
              .withInstances(
                  Instances.builder()
                      .withAdditionalProperty(
                          routerName,
                          InstancesProperty.builder()
                              .withIproto(
                                  Iproto.builder()
                                      .withAdvertise(Advertise.builder().withClient(uri).build())
                                      .withListen(
                                          Arrays.asList(
                                              Listen.builder().withUri(localhostUri).build(),
                                              Listen.builder().withUri(uri).build()))
                                      .build())
                              .build())
                      .build())
              .build());
    }

    final GroupsProperty routerGroup =
        GroupsProperty.builder()
            .withReplication(Replication.builder().withFailover(Failover.MANUAL).build())
            .withSharding(Sharding.builder().withRoles(Collections.singleton(Role.ROUTER)).build())
            .withRoles(Collections.singletonList("roles.crud-router"))
            .withReplicasets(routerReplicasetsBuilder.build())
            .build();

    // r<number_of_shard>-s<number_of_storage> format
    final ReplicasetsBuilderBase<?> shardReplicasetsBuilder = Replicasets.builder();
    for (int i = 1; i <= shardCount; i++) {
      final String shardPrefix = "r" + i;

      final ReplicasetsPropertyBuilderBase<?> replicasetBuilder = ReplicasetsProperty.builder();
      final InstancesBuilderBase<?> instancesBuilder = Instances.builder();
      for (int j = 1; j <= replicaCount; j++) {
        final String replicaName = shardPrefix + "-s" + j;
        final String uri = replicaName + ":" + DEFAULT_IPROTO_TARANTOOL_PORT;
        final String localhostUri = "localhost:" + DEFAULT_IPROTO_TARANTOOL_PORT;
        instancesBuilder.withAdditionalProperty(
            replicaName,
            InstancesProperty.builder()
                .withIproto(
                    Iproto.builder()
                        .withAdvertise(Advertise.builder().withClient(uri).build())
                        .withListen(
                            Arrays.asList(
                                Listen.builder().withUri(uri).build(),
                                Listen.builder().withUri(localhostUri).build()))
                        .build())
                .build());
      }

      final ReplicasetsProperty replicaset =
          replicasetBuilder.withInstances(instancesBuilder.build()).build();
      shardReplicasetsBuilder.withAdditionalProperty(shardPrefix, replicaset);
    }
    final GroupsProperty shardsGroup =
        GroupsProperty.builder()
            .withReplication(Replication.builder().withFailover(Failover.ELECTION).build())
            .withRoles(Collections.singletonList("roles.crud-storage"))
            .withSharding(Sharding.builder().withRoles(Collections.singleton(Role.STORAGE)).build())
            .withReplicasets(shardReplicasetsBuilder.build())
            .build();

    final io.tarantool.autogen.iproto.Iproto iproto =
        io.tarantool.autogen.iproto.Iproto.builder()
            .withAdvertise(
                io.tarantool.autogen.iproto.advertise.Advertise.builder()
                    .withSharding(
                        io.tarantool.autogen.iproto.advertise.sharding.Sharding.builder()
                            .withLogin("storage")
                            .build())
                    .withPeer(
                        io.tarantool.autogen.iproto.advertise.peer.Peer.builder()
                            .withLogin("replicator")
                            .build())
                    .build())
            .build();

    return Tarantool3Configuration.builder()
        .withCredentials(credentials)
        .withGroups(
            Groups.builder()
                .withAdditionalProperty("routers", routerGroup)
                .withAdditionalProperty("storages", shardsGroup)
                .build())
        .withIproto(iproto)
        .build();
  }

  /** Returns all instance names of all groups and replicasets from passed configuration */
  public static List<String> parseInstances(Tarantool3Configuration configuration) {
    return configuration
        .getGroups()
        .orElseThrow(IllegalArgumentException::new)
        .getAdditionalProperties()
        .values()
        .stream()
        .filter(
            g -> {
              if (g == null) {
                throw new IllegalArgumentException("group section value is null");
              }
              return true;
            })
        .flatMap(gp -> parseInstances(gp).stream())
        .collect(Collectors.toList());
  }

  /** Returns names of all instances that have passed role. */
  public static List<String> findInstancesWithRole(Tarantool3Configuration config, String role) {
    final Map<String, List<String>> rolesByInstanceName = parseRolesByInstanceNames(config);
    return rolesByInstanceName.entrySet().stream()
        .filter(entry -> entry.getValue().contains(role))
        .map(Entry::getKey)
        .collect(Collectors.toList());
  }

  /** Returns all instance names in passed replicaset */
  private static Set<String> parseInstances(ReplicasetsProperty replicaset) {
    return replicaset
        .getInstances()
        .orElseThrow(IllegalArgumentException::new)
        .getAdditionalProperties()
        .keySet();
  }

  /** Returns all instance names in passed group */
  private static Set<String> parseInstances(GroupsProperty group) {
    return group
        .getReplicasets()
        .orElseThrow(IllegalArgumentException::new)
        .getAdditionalProperties()
        .values()
        .stream()
        .filter(
            r -> {
              if (r == null) {
                throw new IllegalArgumentException("replicaset section value is null");
              }
              return true;
            })
        .flatMap(rp -> parseInstances(rp).stream())
        .collect(Collectors.toSet());
  }

  /** Returns map with keys are instance name, values are lists of these key's roles */
  private static Map<String, List<String>> parseRolesByInstanceNames(
      Tarantool3Configuration config) {
    final Map<String, List<String>> rolesByInstanceNames = new HashMap<>();

    final List<String> globalRoles = config.getRoles().orElseGet(ArrayList::new);

    final Groups groups = config.getGroups().orElseThrow(IllegalArgumentException::new);
    for (GroupsProperty group : groups.getAdditionalProperties().values()) {
      final List<String> groupRoles = group.getRoles().orElseGet(ArrayList::new);

      final Replicasets replicasets =
          group.getReplicasets().orElseThrow(IllegalArgumentException::new);
      for (ReplicasetsProperty replicaset : replicasets.getAdditionalProperties().values()) {
        final List<String> replicasetRoles = replicaset.getRoles().orElseGet(ArrayList::new);

        final Instances instances =
            replicaset.getInstances().orElseThrow(IllegalArgumentException::new);
        for (Entry<String, InstancesProperty> instanceEntry :
            instances.getAdditionalProperties().entrySet()) {
          final List<String> instanceRoles =
              instanceEntry.getValue().getRoles().orElseGet(ArrayList::new);
          instanceRoles.addAll(replicasetRoles);
          instanceRoles.addAll(groupRoles);
          instanceRoles.addAll(globalRoles);
          rolesByInstanceNames.put(instanceEntry.getKey(), instanceRoles);
        }
      }
    }
    return rolesByInstanceNames;
  }

  /**
   * Bootstraps cluster via router
   *
   * @param bootstrapTimeout bootstrap success timeout
   */
  public static void bootstrap(
      HostPort uri,
      TarantoolContainer<?> router,
      Duration bootstrapTimeout,
      CharSequence user,
      CharSequence password) {

    final StringBuilder sb =
        new StringBuilder(user)
            .append(":")
            .append(password)
            .append("@")
            .append(uri.getHost())
            .append(":")
            .append(uri.getPort());

    LOGGER.info("Start cluster bootstrap in '{}' container", router);
    try {
      Unreliables.retryUntilSuccess(
          (int) bootstrapTimeout.getSeconds(),
          TimeUnit.SECONDS,
          () ->
              Utils.execExceptionally(
                  LOGGER,
                  router.getContainerInfo(),
                  "vshard bootstrap failed",
                  "tt",
                  "replicaset",
                  "vshard",
                  "bootstrap",
                  sb.toString()));
    } catch (Exception exc) {
      throw new ContainerLaunchException("Timed out waiting for cluster to bootstrap", exc);
    }
  }

  /**
   * Adds additional users into global section of passed configuration.
   *
   * @return copy instance of passed configuration
   */
  public static Tarantool3Configuration addUsers(
      Tarantool3Configuration old, Map<String, UsersProperty> users) {
    if (old == null) {
      return null;
    }
    final Tarantool3Configuration copy =
        ConfigurationUtils.create(ConfigurationUtils.writeAsString(old));

    final Credentials credentials = copy.getCredentials().orElseGet(Credentials::new);
    final Users oldUsers = credentials.getUsers().orElseGet(Users::new);

    users.forEach(oldUsers::setAdditionalProperty);

    credentials.setUsers(oldUsers);
    copy.setCredentials(credentials);
    return copy;
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package testcontainers.utils;

// --8<-- [start:create-single-instance]

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.testcontainers.containers.tarantool.config.ConfigurationUtils;

import io.tarantool.autogen.Tarantool3Configuration;
import io.tarantool.autogen.credentials.Credentials;
import io.tarantool.autogen.credentials.users.Users;
import io.tarantool.autogen.credentials.users.usersProperty.UsersProperty;
import io.tarantool.autogen.groups.Groups;
import io.tarantool.autogen.groups.groupsProperty.GroupsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.Replicasets;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.ReplicasetsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.Instances;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.InstancesProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.Iproto;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.listen.Listen;

public class TarantoolSingleNodeConfigUtils {

  public static final String NODE = "test-node";

  public static final CharSequence PWD = "secret";

  public static final String LOGIN = "test-user";

  /*
  // Создает конфигурацию вида:
  // Creates configuration like:
    ---
    credentials:
      users:
        test-user:
          password: "secret"
          roles:
          - "super"
    groups:
      test-group:
        replicasets:
          test-rs:
            instances:
              test-node:
                iproto:
                  listen:
                  - uri: "0.0.0.0:3301"
   */
  public static Path createConfig(Path tempDir) throws IOException {
    final Path pathToConfigFile = Files.createFile(tempDir.resolve("config.yaml"));

    final Credentials credentials =
        Credentials.builder()
            .withUsers(
                Users.builder()
                    .withAdditionalProperty(
                        LOGIN,
                        UsersProperty.builder()
                            .withRoles(Collections.singletonList("super"))
                            .withPassword(PWD.toString())
                            .build())
                    .build())
            .build();

    final Iproto iproto =
        Iproto.builder()
            .withListen(Collections.singletonList(Listen.builder().withUri("0.0.0.0:3301").build()))
            .build();

    final InstancesProperty instance = InstancesProperty.builder().withIproto(iproto).build();

    final ReplicasetsProperty replicaset =
        ReplicasetsProperty.builder()
            .withInstances(Instances.builder().withAdditionalProperty(NODE, instance).build())
            .build();

    final GroupsProperty group =
        GroupsProperty.builder()
            .withReplicasets(
                Replicasets.builder().withAdditionalProperty("test-rs", replicaset).build())
            .build();

    final Tarantool3Configuration configuration =
        Tarantool3Configuration.builder()
            .withGroups(Groups.builder().withAdditionalProperty("test-group", group).build())
            .withCredentials(credentials)
            .build();

    ConfigurationUtils.writeToFile(configuration, pathToConfigFile);
    return pathToConfigFile;
  }
}

// --8<-- [end:create-single-instance]

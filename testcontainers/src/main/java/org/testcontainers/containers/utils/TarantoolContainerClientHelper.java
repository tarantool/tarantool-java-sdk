/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.testcontainers.containers.utils.PathUtils.normalizePath;
import lombok.SneakyThrows;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.TarantoolContainerOperations;
import org.testcontainers.containers.tarantool.Tarantool2Container;
import org.testcontainers.containers.tarantool.Tarantool3Container;
import org.testcontainers.containers.tarantool.Tarantool3WaitStrategy;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.tarantool.config.ConfigurationUtils;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.yaml.snakeyaml.Yaml;

import io.tarantool.autogen.Tarantool3Configuration;
import io.tarantool.autogen.credentials.Credentials;
import io.tarantool.autogen.credentials.roles.rolesProperty.privilege.Permission;
import io.tarantool.autogen.credentials.roles.rolesProperty.privilege.Privilege;
import io.tarantool.autogen.credentials.users.Users;
import io.tarantool.autogen.credentials.users.usersProperty.UsersProperty;
import io.tarantool.autogen.groups.Groups;
import io.tarantool.autogen.groups.groupsProperty.GroupsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.Replicasets;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.ReplicasetsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.Instances;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.InstancesProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.Iproto;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.advertise.Advertise;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.advertise.peer.Peer;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.listen.Listen;

/**
 * Provides a wrapper around a Tarantool client with helper methods
 *
 * @author Alexey Kuzin
 * @author Artyom Dubinin
 * @author Ivan Dneprov
 */
public final class TarantoolContainerClientHelper {

  private static final String TMP_DIR = "/tmp";
  private static final Yaml yaml = new Yaml();
  public static final String TARANTOOL_VERSION =
      System.getenv().getOrDefault("TARANTOOL_VERSION", "2.11.2-ubuntu20.04");
  private static final String TT_COMMAND =
      TARANTOOL_VERSION.startsWith("2.") ? "tarantoolctl" : "tt";
  public static final String IMAGE_PREFIX =
      System.getenv().getOrDefault("TARANTOOL_REGISTRY", "") + "tarantool/tarantool";
  public static final DockerImageName DOCKER_IMAGE =
      DockerImageName.parse(String.format("%s:%s", IMAGE_PREFIX, TARANTOOL_VERSION));
  private static final Network NETWORK = Network.newNetwork();

  /**
   * Ports used by proxy servers in tarantool3/init.lua (socket.tcp_server). Tarantool must not
   * listen on these ports so that init.lua can bind them.
   */
  private static final Set<Integer> PROXY_PORTS = Set.of(3304, 3305, 3306);

  private static final String EXECUTE_SCRIPT_ERROR_TEMPLATE =
      "Executed script %s with exit code %d, stderr: \"%s\", stdout: \"%s\"";
  private static final String EXECUTE_COMMAND_ERROR_TEMPLATE =
      "Executed command \"%s\" with exit code %d, stderr: \"%s\", stdout: \"%s\"";

  private static final String ECHO_COMMAND = "echo \"%s\" | %s connect %s:%s@%s:%d";
  private static final String COMMAND_TEMPLATE =
      "echo \" "
          + "    print(require('yaml').encode( "
          + "        {require('net.box').connect( "
          + "            '%s:%d',  "
          + "            { user = '%s', password = '%s' } "
          + "            ):eval('%s')}) "
          + "        ); "
          + "    os.exit(); "
          + "\" > container-tmp.lua &&"
          + " tarantool container-tmp.lua";
  private static final String MTLS_COMMAND_TEMPLATE =
      "echo \"     print(require('yaml').encode(         {require('net.box').connect(             {"
          + " uri='%s:%d', params = { transport='ssl', ssl_key_file = '%s', ssl_cert_file = '%s'"
          + " }},              { user = '%s', password = '%s' }             ):eval('%s')})        "
          + " );     os.exit(); \" > container-tmp.lua && tarantool container-tmp.lua";
  private static final String SSL_COMMAND_TEMPLATE =
      "echo \" "
          + "    print(require('yaml').encode( "
          + "        {require('net.box').connect( "
          + "            { uri='%s:%d', params = { transport='ssl' }},  "
          + "            { user = '%s', password = '%s' } "
          + "            ):eval('%s')}) "
          + "        ); "
          + "    os.exit(); "
          + "\" > container-tmp.lua &&"
          + " tarantool container-tmp.lua";

  private static final String API_USER = "api_user";

  private static final Map<String, String> CREDS =
      new HashMap<>() {
        {
          put(API_USER, "secret");
        }
      };

  public static Container.ExecResult executeScript(
      TarantoolContainer<?> container, String scriptResourcePath)
      throws IOException, InterruptedException {
    if (!container.isRunning()) {
      throw new IllegalStateException("Cannot execute scripts in stopped container");
    }

    String scriptName = Paths.get(scriptResourcePath).getFileName().toString();
    String containerPath = normalizePath(Paths.get(TMP_DIR, scriptName));
    container.copyFileToContainer(
        MountableFile.forClasspathResource(scriptResourcePath), containerPath);
    return executeCommand(container, String.format("return dofile('%s')", containerPath));
  }

  public static <T> T executeScriptDecoded(
      TarantoolContainer<?> container, String scriptResourcePath)
      throws IOException, InterruptedException, ExecutionException {
    Container.ExecResult result = executeScript(container, scriptResourcePath);

    if (result.getExitCode() != 0) {

      String message =
          String.format(
              EXECUTE_SCRIPT_ERROR_TEMPLATE,
              scriptResourcePath,
              result.getExitCode(),
              result.getStderr(),
              result.getStdout());

      if (result.getExitCode() == 3 || result.getExitCode() == 1) {
        throw new ExecutionException(message, new Throwable());
      }

      throw new IllegalStateException(message);
    }

    return yaml.load(result.getStdout());
  }

  public static Container.ExecResult executeCommand(TarantoolContainer<?> container, String command)
      throws IOException, InterruptedException {
    if (!container.isRunning()) {
      throw new IllegalStateException("Cannot execute commands in stopped container");
    }

    command = command.replace("\"", "\\\"");

    String bashCommand =
        String.format(
            ECHO_COMMAND, command, TT_COMMAND, API_USER, CREDS.get(API_USER), "localhost", 3301);

    return container.execInContainer("/bin/sh", "-c", bashCommand);
  }

  public static Container.ExecResult executeCommand(
      TarantoolContainer<?> container, String command, int port)
      throws IOException, InterruptedException {
    if (!container.isRunning()) {
      throw new IllegalStateException("Cannot execute commands in stopped container");
    }

    command = command.replace("\"", "\\\"");

    String bashCommand =
        String.format(
            ECHO_COMMAND, command, TT_COMMAND, API_USER, CREDS.get(API_USER), "localhost", port);

    return container.execInContainer("/bin/sh", "-c", bashCommand);
  }

  public static <T> T executeCommandDecoded(
      TarantoolContainer<?> container, String command, int port)
      throws IOException, InterruptedException {
    Container.ExecResult result = executeCommand(container, command, port);

    if (result.getExitCode() != 0) {
      throw new RuntimeException(result.getStderr() + "\n" + command);
    }

    return yaml.load(result.getStdout());
  }

  public static <T> T executeCommandDecoded(TarantoolContainer<?> container, String command)
      throws IOException, InterruptedException {
    Container.ExecResult result = executeCommand(container, command);

    if (result.getExitCode() != 0) {
      throw new RuntimeException(result.getStderr() + "\n" + command);
    }

    return yaml.load(result.getStdout());
  }

  public static TarantoolContainer<?> createTarantoolContainer(Integer... exposedPorts) {
    Path initScriptPath = null;
    TarantoolContainer<?> container;
    try {
      initScriptPath =
          Paths.get(
              Objects.requireNonNull(
                      TarantoolContainerClientHelper.class
                          .getClassLoader()
                          .getResource("server.lua"))
                  .toURI());
    } catch (Exception e) {
      // ignore
    }

    String containerName = String.format("node-%s", UUID.randomUUID());
    container =
        switch (Character.getNumericValue(TARANTOOL_VERSION.charAt(0))) {
          case 2 -> Tarantool2Container.builder(DOCKER_IMAGE, initScriptPath).build();
          case 3 ->
              new Tarantool3Container(DOCKER_IMAGE, containerName)
                  .withConfigPath(createConfig(containerName, exposedPorts))
                  .withNetwork(NETWORK)
                  .waitingFor(
                      new Tarantool3WaitStrategy("localhost", API_USER, CREDS.get(API_USER)))
                  .withPrivilegedMode(true)
                  .withCreateContainerCmdModifier(cmd -> cmd.withUser("root"));
          default ->
              throw new RuntimeException(
                  String.format("Unsupported Tarantool version, %s", TARANTOOL_VERSION));
        };

    return container.withExposedPorts(3301).withExposedPorts(exposedPorts);
  }

  @SneakyThrows
  private static Path createConfig(String containerName, Integer... exposedPorts) {
    final Path pathToConfigFile =
        Files.createFile(Paths.get(TMP_DIR).resolve(String.format("%s.yaml", UUID.randomUUID())));

    final Credentials credentials =
        Credentials.builder()
            .withUsers(
                Users.builder()
                    .withAdditionalProperty(
                        API_USER,
                        UsersProperty.builder()
                            .withRoles(Collections.singletonList("super"))
                            .withPassword(CREDS.get(API_USER))
                            .build())
                    .withAdditionalProperty(
                        "user_a",
                        UsersProperty.builder()
                            .withRoles(Collections.singletonList("super"))
                            .withPassword("secret_a")
                            .withPrivileges(
                                List.of(
                                    Privilege.builder()
                                        .withPermissions(
                                            new LinkedHashSet<>(
                                                List.of(
                                                    Permission.READ,
                                                    Permission.WRITE,
                                                    Permission.EXECUTE)))
                                        .build()))
                            .build())
                    .withAdditionalProperty(
                        "user_b",
                        UsersProperty.builder()
                            .withPassword("secret_b")
                            .withPrivileges(
                                List.of(
                                    Privilege.builder()
                                        .withPermissions(
                                            new LinkedHashSet<>(
                                                List.of(
                                                    Permission.READ,
                                                    Permission.WRITE,
                                                    Permission.EXECUTE)))
                                        .build()))
                            .build())
                    .withAdditionalProperty(
                        "user_c", UsersProperty.builder().withPassword("secret_c").build())
                    .withAdditionalProperty(
                        "user_d",
                        UsersProperty.builder()
                            .withPassword("secret_d")
                            .withPrivileges(
                                Collections.singletonList(
                                    Privilege.builder()
                                        .withPermissions(
                                            new LinkedHashSet<>(
                                                Collections.singletonList(Permission.EXECUTE)))
                                        .build()))
                            .build())
                    .withAdditionalProperty(
                        "replicator",
                        UsersProperty.builder()
                            .withPassword("password")
                            .withRoles(Collections.singletonList("replication"))
                            .build())
                    .build())
            .build();

    final Iproto.IprotoBuilderBase<?> iprotoBuilder = Iproto.builder();
    final List<Listen> listens =
        new ArrayList<>() {
          {
            add(Listen.builder().withUri("0.0.0.0:3301").build());
          }
        };

    for (int port : exposedPorts) {
      if (!PROXY_PORTS.contains(port)) {
        listens.add(Listen.builder().withUri(String.format("0.0.0.0:%d", port)).build());
      }
    }

    iprotoBuilder
        .withListen(listens)
        .withAdvertise(
            Advertise.builder().withPeer(Peer.builder().withLogin("replicator").build()).build());

    final InstancesProperty instance =
        InstancesProperty.builder().withIproto(iprotoBuilder.build()).build();

    final ReplicasetsProperty replicaset =
        ReplicasetsProperty.builder()
            .withInstances(
                Instances.builder().withAdditionalProperty(containerName, instance).build())
            .build();

    final GroupsProperty group =
        GroupsProperty.builder()
            .withReplicasets(
                Replicasets.builder()
                    .withAdditionalProperty(containerName + "-rs", replicaset)
                    .build())
            .build();

    final Tarantool3Configuration configuration =
        Tarantool3Configuration.builder()
            .withGroups(
                Groups.builder().withAdditionalProperty(containerName + "-group", group).build())
            .withCredentials(credentials)
            .build();

    ConfigurationUtils.writeToFile(configuration, pathToConfigFile);
    return pathToConfigFile;
  }

  public static Container.ExecResult execInitScript(TarantoolContainer<?> container) {
    try {
      return executeScript(container, "tarantool3/init.lua");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static Container.ExecResult executeScript(
      TarantoolContainerOperations<?> container, String scriptResourcePath, SslContext sslContext)
      throws IOException, InterruptedException {
    if (!container.isRunning()) {
      throw new IllegalStateException("Cannot execute scripts in stopped container");
    }

    String scriptName = Paths.get(scriptResourcePath).getFileName().toString();
    String containerPath = normalizePath(Paths.get(TMP_DIR, scriptName));
    container.copyFileToContainer(
        MountableFile.forClasspathResource(scriptResourcePath), containerPath);
    return executeCommand(
        container, String.format("return dofile('%s')", containerPath), sslContext);
  }

  public static <T> T executeScriptDecoded(
      TarantoolContainerOperations<?> container, String scriptResourcePath, SslContext sslContext)
      throws IOException, InterruptedException, ExecutionException {
    Container.ExecResult result = executeScript(container, scriptResourcePath, sslContext);

    if (result.getExitCode() != 0) {

      if (result.getExitCode() == 3 || result.getExitCode() == 1) {
        throw new ExecutionException(
            String.format(
                EXECUTE_SCRIPT_ERROR_TEMPLATE,
                scriptResourcePath,
                result.getExitCode(),
                result.getStderr(),
                result.getStdout()),
            new Throwable());
      }

      throw new IllegalStateException(
          String.format(
              EXECUTE_SCRIPT_ERROR_TEMPLATE,
              scriptResourcePath,
              result.getExitCode(),
              result.getStderr(),
              result.getStdout()));
    }

    return yaml.load(result.getStdout());
  }

  public static Container.ExecResult executeCommand(
      TarantoolContainerOperations<?> container, String command, SslContext sslContext)
      throws IOException, InterruptedException {
    if (!container.isRunning()) {
      throw new IllegalStateException("Cannot execute commands in stopped container");
    }

    command = command.replace("\"", "\\\"");
    command = command.replace("\'", "\\\'");

    String bashCommand;
    if (sslContext == null) { // No SSL
      bashCommand =
          String.format(
              COMMAND_TEMPLATE,
              container.getHost(),
              container.getInternalPort(),
              container.getUsername(),
              container.getPassword(),
              command);
    } else if (sslContext.getKeyFile() != null && sslContext.getCertFile() != null) { // mTLS
      bashCommand =
          String.format(
              MTLS_COMMAND_TEMPLATE,
              container.getHost(),
              container.getInternalPort(),
              sslContext.getKeyFile(),
              sslContext.getCertFile(),
              container.getUsername(),
              container.getPassword(),
              command);
    } else { // SSL
      bashCommand =
          String.format(
              SSL_COMMAND_TEMPLATE,
              container.getHost(),
              container.getInternalPort(),
              container.getUsername(),
              container.getPassword(),
              command);
    }

    return container.execInContainer("sh", "-c", bashCommand);
  }

  public static <T> T executeCommandDecoded(
      TarantoolContainerOperations<?> container, String command, SslContext sslContext)
      throws IOException, InterruptedException {
    Container.ExecResult result = executeCommand(container, command, sslContext);

    if (result.getExitCode() != 0) {
      throw new IllegalStateException(
          String.format(
              EXECUTE_COMMAND_ERROR_TEMPLATE,
              command,
              result.getExitCode(),
              result.getStderr(),
              result.getStdout()));
    }

    return yaml.load(result.getStdout());
  }
}

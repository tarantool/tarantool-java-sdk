/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
  public static final String TARANTOOL_VERSION = System.getenv("TARANTOOL_VERSION");
  public static final String IMAGE_PREFIX =
      System.getenv().getOrDefault("TARANTOOL_REGISTRY", "") + "tarantool/tarantool";
  private static final Network NETWORK = Network.newNetwork();

  private static final String EXECUTE_SCRIPT_ERROR_TEMPLATE =
      "Executed script %s with exit code %d, stderr: \"%s\", stdout: \"%s\"";
  private static final String EXECUTE_COMMAND_ERROR_TEMPLATE =
      "Executed command \"%s\" with exit code %d, stderr: \"%s\", stdout: \"%s\"";

  private static final String ECHO_COMMAND = "echo \"%s\" | tarantoolctl connect %s:%s@%s:%d";
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

  private static final String API_USER = "api_user";

  private static final Map<String, String> CREDS =
      new HashMap<>() {
        {
          put(API_USER, "secret");
        }
      };

  public static Container.ExecResult executeScript(
      TarantoolContainer<?> container, String scriptResourcePath, SslContext sslContext)
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
      TarantoolContainer<?> container, String scriptResourcePath, SslContext sslContext)
      throws IOException, InterruptedException, ExecutionException {
    Container.ExecResult result = executeScript(container, scriptResourcePath, sslContext);

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
        String.format(ECHO_COMMAND, command, API_USER, CREDS.get(API_USER), "localhost", 3301);

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
        String.format(ECHO_COMMAND, command, API_USER, CREDS.get(API_USER), "localhost", port);

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

  public static TarantoolContainer<?> createTarantoolContainer() {
    DockerImageName dockerImage =
        DockerImageName.parse(String.format("%s:%s", IMAGE_PREFIX, TARANTOOL_VERSION));
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

    container =
        switch (Character.getNumericValue(TARANTOOL_VERSION.charAt(0))) {
          case 2 -> Tarantool2Container.builder(dockerImage, initScriptPath).build();
          case 3 ->
              new Tarantool3Container(dockerImage, "test-node")
                  .withConfigPath(createConfig())
                  .withNetwork(NETWORK)
                  .waitingFor(
                      new Tarantool3WaitStrategy("localhost", API_USER, CREDS.get(API_USER)));
          default ->
              throw new RuntimeException(
                  String.format("Unsupported Tarantool version, %s", TARANTOOL_VERSION));
        };

    return container;
  }

  @SneakyThrows
  private static Path createConfig() {
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
                    .build())
            .build();

    final Iproto iproto =
        Iproto.builder()
            .withListen(Collections.singletonList(Listen.builder().withUri("0.0.0.0:3301").build()))
            .build();

    final InstancesProperty instance = InstancesProperty.builder().withIproto(iproto).build();

    final ReplicasetsProperty replicaset =
        ReplicasetsProperty.builder()
            .withInstances(
                Instances.builder().withAdditionalProperty("test-node", instance).build())
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

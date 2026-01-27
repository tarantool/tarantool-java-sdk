/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool;

import java.io.IOException;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.utils.SslContext;
import org.yaml.snakeyaml.Yaml;

public final class TarantoolContainerLuaExecutor {

  private static final String EXECUTE_COMMAND_ERROR_TEMPLATE =
      "Executed command \"%s\" with exit code %d, stderr: \"%s\", stdout: \"%s\"";
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

  private static final String ENV_USERNAME_CMD =
      "echo ${TARANTOOL_USER_NAME:-${TT_CLI_USERNAME:-guest}}";
  private static final String ENV_PASSWORD_CMD =
      "echo ${TARANTOOL_USER_PASSWORD:-${TT_CLI_PASSWORD:-}}";

  private static final Yaml YAML = new Yaml();

  private final Container<?> container;
  private final int port;

  public TarantoolContainerLuaExecutor(Container<?> container, int port) {
    this.container = container;
    this.port = port;
  }

  public String getExecResult(String command) throws Exception {
    Container.ExecResult result = this.container.execInContainer(command);
    if (result.getExitCode() != 0) {
      throw new RuntimeException("Cannot execute script: " + command);
    }
    return result.getStdout().trim().replace("\n", "").replace("...", "").replace("--", "").trim();
  }

  public Container.ExecResult executeCommand(String command)
      throws IOException, InterruptedException {
    return executeCommand(command, null);
  }

  public Container.ExecResult executeCommand(String command, SslContext sslContext)
      throws IOException, InterruptedException {
    if (!container.isRunning()) {
      throw new IllegalStateException("Cannot execute commands in stopped container");
    }

    command = command.replace("\"", "\\\"");
    command = command.replace("\'", "\\\'");

    String username = getUsernameFromEnv();
    String password = getPasswordFromEnv();
    String host = "localhost";

    String bashCommand;
    if (sslContext == null) {
      bashCommand = String.format(COMMAND_TEMPLATE, host, port, username, password, command);
    } else if (sslContext.getKeyFile() != null && sslContext.getCertFile() != null) {
      bashCommand =
          String.format(
              MTLS_COMMAND_TEMPLATE,
              host,
              port,
              sslContext.getKeyFile(),
              sslContext.getCertFile(),
              username,
              password,
              command);
    } else {
      bashCommand = String.format(SSL_COMMAND_TEMPLATE, host, port, username, password, command);
    }

    return container.execInContainer("sh", "-c", bashCommand);
  }

  public <T> T executeCommandDecoded(String command) throws IOException, InterruptedException {
    return executeCommandDecoded(command, null);
  }

  public <T> T executeCommandDecoded(String command, SslContext sslContext)
      throws IOException, InterruptedException {
    Container.ExecResult result = executeCommand(command, sslContext);

    if (result.getExitCode() != 0) {
      throw new IllegalStateException(
          String.format(
              EXECUTE_COMMAND_ERROR_TEMPLATE,
              command,
              result.getExitCode(),
              result.getStderr(),
              result.getStdout()));
    }

    return YAML.load(result.getStdout());
  }

  private String getUsernameFromEnv() throws IOException, InterruptedException {
    Container.ExecResult result = container.execInContainer("sh", "-c", ENV_USERNAME_CMD);
    if (result.getExitCode() != 0) {
      return "guest";
    }
    String username = result.getStdout().trim();
    return username.isEmpty() ? "guest" : username;
  }

  private String getPasswordFromEnv() throws IOException, InterruptedException {
    Container.ExecResult result = container.execInContainer("sh", "-c", ENV_PASSWORD_CMD);
    if (result.getExitCode() != 0) {
      return "";
    }
    return result.getStdout().trim();
  }
}

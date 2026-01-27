/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import static org.testcontainers.containers.utils.PathUtils.normalizePath;
import org.testcontainers.containers.utils.SslContext;
import org.testcontainers.utility.MountableFile;
import org.yaml.snakeyaml.Yaml;

/** Helper class for executing commands on Tarantool containers. */
public final class TarantoolContainerClientHelper {

  private static final String TMP_DIR = "/tmp";
  private static final Yaml yaml = new Yaml();

  private final TarantoolContainerOperations<? extends Container<?>> container;
  private final String EXECUTE_SCRIPT_ERROR_TEMPLATE =
      "Executed script %s with exit code %d, stderr: \"%s\", stdout: \"%s\"";
  private static final String EXECUTE_COMMAND_ERROR_TEMPLATE =
      "Executed command \"%s\" with exit code %d, stderr: \"%s\", stdout: \"%s\"";
  // Generates bash command witch creates executable lua file with connection to required node
  // and evaluation of needed lua code
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

  public TarantoolContainerClientHelper(
      TarantoolContainerOperations<? extends Container<?>> container) {
    this.container = container;
  }

  public Container.ExecResult executeScript(String scriptResourcePath, SslContext sslContext)
      throws IOException, InterruptedException {
    if (!container.isRunning()) {
      throw new IllegalStateException("Cannot execute scripts in stopped container");
    }

    String scriptName = Paths.get(scriptResourcePath).getFileName().toString();
    String containerPath = normalizePath(Paths.get(TMP_DIR, scriptName));
    container.copyFileToContainer(
        MountableFile.forClasspathResource(scriptResourcePath), containerPath);
    return executeCommand(String.format("return dofile('%s')", containerPath), sslContext);
  }

  public <T> T executeScriptDecoded(String scriptResourcePath, SslContext sslContext)
      throws IOException, InterruptedException, ExecutionException {
    Container.ExecResult result = executeScript(scriptResourcePath, sslContext);

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

  public Container.ExecResult executeCommand(String command, SslContext sslContext)
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

    return yaml.load(result.getStdout());
  }
}

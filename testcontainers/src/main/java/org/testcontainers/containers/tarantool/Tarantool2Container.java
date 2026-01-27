/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.utils.Utils;
import org.testcontainers.utility.DockerImageName;

public class Tarantool2Container extends GenericContainer<Tarantool2Container>
    implements TarantoolContainer<Tarantool2Container> {

  private final String initScript;

  private final String node;

  private final Path mountPath;

  private boolean isClosed;

  private boolean configured;

  private final TarantoolContainerLuaExecutor luaExecutor;

  private Tarantool2Container(DockerImageName dockerImageName, String initScript, String node) {
    super(dockerImageName);
    this.node = node;
    this.initScript = initScript;
    this.mountPath = Utils.createTempDirectory(this.node);
    this.luaExecutor =
        new TarantoolContainerLuaExecutor(this, TarantoolContainer.DEFAULT_TARANTOOL_PORT);
  }

  @Override
  protected void configure() {
    if (configured) {
      return;
    }

    try {
      final String initScriptName = "init.lua";

      final Path initialScriptOnHost = this.mountPath.resolve(initScriptName);
      final Path initialScriptOnContainer = DEFAULT_DATA_DIR.resolve(initScriptName);

      Files.write(initialScriptOnHost, this.initScript.getBytes(StandardCharsets.UTF_8));

      withCreateContainerCmdModifier(cmd -> cmd.withName(this.node));
      withNetworkAliases(this.node);

      addFileSystemBind(
          this.mountPath.toAbsolutePath().toString(),
          DEFAULT_DATA_DIR.toAbsolutePath().toString(),
          BindMode.READ_WRITE,
          SelinuxContext.SHARED);
      addExposedPort(DEFAULT_TARANTOOL_PORT);

      addEnv("TT_MEMTX_DIR", DEFAULT_DATA_DIR.toAbsolutePath().toString());
      addEnv("TT_WAL_DIR", DEFAULT_DATA_DIR.toAbsolutePath().toString());
      addEnv("TT_VINYL_DIR", DEFAULT_DATA_DIR.toAbsolutePath().toString());
      addEnv("TT_LISTEN", String.valueOf(DEFAULT_TARANTOOL_PORT));

      setCommand(
          "/bin/sh",
          "-c",
          String.format("tarantool %s", initialScriptOnContainer.toAbsolutePath()));
      this.configured = true;
    } catch (IOException e) {
      throw new ContainerLaunchException("Tarantool 2 container doesn't start", e);
    }
  }

  @Override
  public synchronized void start() {
    if (this.isClosed) {
      throw new ContainerLaunchException(
          "Container is already closed. Please create new container");
    }
    super.start();
  }

  @Override
  public TarantoolContainer<Tarantool2Container> withConfigPath(Path configPath) {
    throw new UnsupportedOperationException("Tarantool2Container doesn't support this method");
  }

  @Override
  public TarantoolContainer<Tarantool2Container> withMigrationsPath(Path migrationsPath) {
    throw new UnsupportedOperationException("Tarantool2Container doesn't support this method");
  }

  @Override
  public String node() {
    return this.node;
  }

  @Override
  public InetSocketAddress mappedAddress() {
    return new InetSocketAddress(
        getHost(), getMappedPort(TarantoolContainer.DEFAULT_TARANTOOL_PORT));
  }

  @Override
  public synchronized void stopWithSafeMount() {
    if (this.isClosed) {
      return;
    }
    super.stop();
  }

  @Override
  protected void containerIsStarted(InspectContainerResponse containerInfo) {
    Utils.bindExposedPorts(this);
  }

  @Override
  public synchronized void stop() {
    if (this.isClosed) {
      return;
    }

    Utils.deleteDataDirectory(this.mountPath);
    super.stop();
    this.isClosed = true;
  }

  @Override
  public InetSocketAddress internalAddress() {
    return new InetSocketAddress(this.node, TarantoolContainer.DEFAULT_TARANTOOL_PORT);
  }

  public String getExecResult(String command) throws Exception {
    return this.luaExecutor.getExecResult(command);
  }

  public Container.ExecResult executeCommand(String command)
      throws IOException, InterruptedException {
    return luaExecutor.executeCommand(command);
  }

  public Container.ExecResult executeCommand(
      String command, org.testcontainers.containers.utils.SslContext sslContext)
      throws IOException, InterruptedException {
    return luaExecutor.executeCommand(command, sslContext);
  }

  public <T> T executeCommandDecoded(String command) throws IOException, InterruptedException {
    return luaExecutor.executeCommandDecoded(command);
  }

  public <T> T executeCommandDecoded(
      String command, org.testcontainers.containers.utils.SslContext sslContext)
      throws IOException, InterruptedException {
    return luaExecutor.executeCommandDecoded(command, sslContext);
  }

  public static Builder builder(DockerImageName image, Path initScriptPath) {
    try {
      final String rawScript =
          new String(Files.readAllBytes(initScriptPath), StandardCharsets.UTF_8);
      return builder(image, rawScript);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Builder builder(DockerImageName dockerImageName, String initScript) {
    return new Builder(dockerImageName, initScript);
  }

  public static class Builder {

    private final DockerImageName dockerImageName;

    private final String initScript;

    private String node;

    public Builder(DockerImageName dockerImageName, String initScript) {
      this.dockerImageName = dockerImageName;
      this.initScript = initScript;
    }

    public Builder withNode(String node) {
      this.node = node;
      return this;
    }

    public Tarantool2Container build() {
      validateName(this.node);
      final String totalNodeName =
          this.node == null ? "tarantool-2.11.x-" + UUID.randomUUID() : this.node;
      return new Tarantool2Container(dockerImageName, this.initScript, totalNodeName);
    }

    private static void validateName(String node) {
      if (node == null) {
        return;
      }

      if (node.isBlank()) {
        throw new ContainerLaunchException("instance name can't be blank");
      }
    }
  }
}

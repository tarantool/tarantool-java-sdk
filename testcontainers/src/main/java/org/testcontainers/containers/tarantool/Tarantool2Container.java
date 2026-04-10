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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.utils.Utils;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Testcontainers for Tarantool version 2.11.x.
 *
 * @implNote The implementation assumes that you will not configure the following parameters in the
 *     init script (they adjusted automatically):
 *     <ul>
 *       <li>{@code listen}
 *       <li>{@code memtx_dir}
 *       <li>{@code wal_dir}
 *       <li>{@code vinyl_dir}
 *     </ul>
 */
public class Tarantool2Container extends GenericContainer<Tarantool2Container>
    implements TarantoolContainer<Tarantool2Container> {

  private final String initScript;
  private final List<Path> additionalScriptPaths;

  private final String node;

  private final Path mountPath;

  private boolean isClosed;

  private boolean configured;

  private Tarantool2Container(
      DockerImageName dockerImageName,
      String initScript,
      List<Path> additionalScriptPaths,
      String node) {
    super(dockerImageName);
    this.node = node;
    this.initScript = initScript;
    this.additionalScriptPaths = new ArrayList<>(additionalScriptPaths);
    this.mountPath = Utils.createTempDirectory(this.node);
  }

  /**
   * @implNote We copy all files from the mounted directory to the container. This is necessary so
   *     that all monitored files got into a container while working in DinD. Mounting in such an
   *     environment follows the following rules:
   *     <p>
   *     <ul>
   *       <li>When we run tests on a system where dockerd is located locally. Mounting occurs
   *           relatively the current host (where dockerd is located).
   *       <li>When we run tests using DinD as a separate service: the tests are executed in the
   *           so-called build-container, which contains all test files and data. Test containers
   *           (tarantool container), which are created testcontainers are launched inside the
   *           so-called DinD container. Mounting is performed relative to the directories of the
   *           DinD container, and not relative to the build container. For this reason we use
   *           additional copying of the mounted directory so that the files in the mounted
   *           directory were also on the DinD container.
   *     </ul>
   */
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
      copyAdditionalScripts(this.additionalScriptPaths, this.mountPath);

      withCreateContainerCmdModifier(cmd -> cmd.withName(this.node).withUser("root"));
      withNetworkAliases(this.node);

      addFileSystemBind(
          this.mountPath.toAbsolutePath().toString(),
          DEFAULT_DATA_DIR.toAbsolutePath().toString(),
          BindMode.READ_WRITE,
          SelinuxContext.SHARED);

      withCopyFileToContainer(
          MountableFile.forHostPath(this.mountPath),
          TarantoolContainer.DEFAULT_DATA_DIR.toAbsolutePath().toString());

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
  public TarantoolContainer<Tarantool2Container> withFixedExposedPort(
      int hostPort, int containerPort) {
    this.addFixedExposedPort(hostPort, containerPort);
    return this;
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

  public List<Path> getAdditionalScriptPaths() {
    return Collections.unmodifiableList(this.additionalScriptPaths);
  }

  public static Builder builder(DockerImageName image, Path initScriptPath) {
    return builder(image, initScriptPath, Collections.emptyList());
  }

  public static Builder builder(
      DockerImageName image, Path initScriptPath, List<Path> additionalScriptPaths) {
    try {
      final String rawScript =
          new String(Files.readAllBytes(initScriptPath), StandardCharsets.UTF_8);
      return builder(image, rawScript, additionalScriptPaths);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Builder builder(DockerImageName dockerImageName, String initScript) {
    return builder(dockerImageName, initScript, Collections.emptyList());
  }

  public static Builder builder(
      DockerImageName dockerImageName, String initScript, List<Path> additionalScriptPaths) {
    return new Builder(dockerImageName, initScript, additionalScriptPaths);
  }

  public static class Builder {

    private final DockerImageName dockerImageName;

    private final String initScript;
    private List<Path> additionalScriptPaths;

    private String node;

    public Builder(
        DockerImageName dockerImageName, String initScript, List<Path> additionalScriptPaths) {
      this.dockerImageName = dockerImageName;
      this.initScript = initScript;
      this.additionalScriptPaths =
          additionalScriptPaths == null ? Collections.emptyList() : additionalScriptPaths;
    }

    public Builder withNode(String node) {
      this.node = node;
      return this;
    }

    public Builder withAdditionalScriptPaths(List<Path> additionalScriptPaths) {
      this.additionalScriptPaths =
          additionalScriptPaths == null ? Collections.emptyList() : additionalScriptPaths;
      return this;
    }

    public Tarantool2Container build() {
      validateName(this.node);
      final String totalNodeName =
          this.node == null ? "tarantool-2.11.x-" + UUID.randomUUID() : this.node;
      return new Tarantool2Container(
          dockerImageName, this.initScript, this.additionalScriptPaths, totalNodeName);
    }

    private static void validateName(String node) {
      if (node == null) {
        return;
      }

      if (node.strip().isEmpty()) {
        throw new ContainerLaunchException("instance name can't be blank");
      }
    }
  }

  private static void copyAdditionalScripts(List<Path> additionalScriptPaths, Path mountPath)
      throws IOException {
    if (additionalScriptPaths == null || additionalScriptPaths.isEmpty()) {
      return;
    }

    for (Path additionalScriptPath : additionalScriptPaths) {
      if (additionalScriptPath == null) {
        throw new IllegalArgumentException("Additional script path must not be null");
      }
      if (!Files.isRegularFile(additionalScriptPath)) {
        throw new IllegalArgumentException(
            String.format(
                "Additional script path '%s' must point to an existing regular file",
                additionalScriptPath));
      }
      Path targetOnHost = mountPath.resolve(additionalScriptPath.getFileName());
      Files.copy(additionalScriptPath, targetOnHost, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}

/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.utils.HttpHost;
import org.testcontainers.containers.utils.Utils;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class Tarantool3Container extends GenericContainer<Tarantool3Container>
    implements TarantoolContainer<Tarantool3Container> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Tarantool3Container.class);

  private final UUID instanceUuid;

  private final String node;

  private final Lock lock;

  private final AtomicBoolean isClosed;

  private final List<HttpHost> etcdAddresses;

  private String etcdPrefix;

  private Path mountDataDirectory;

  private Path configPath;

  private Path migrationsPath;

  private boolean configured;

  public Tarantool3Container(DockerImageName dockerImageName, String node) {
    super(dockerImageName);
    this.instanceUuid = UUID.randomUUID();
    if (Utils.isNullOrBlank(node)) {
      throw new IllegalArgumentException("Instance name (node) cannot be `null` or `blank`");
    }
    this.node = node;
    this.lock = new ReentrantLock();
    this.isClosed = new AtomicBoolean();
    this.etcdAddresses = new ArrayList<>(1);
  }

  public Tarantool3Container withEtcdAddresses(HttpHost... etcdAddresses) {
    try {
      this.lock.lock();
      this.etcdAddresses.addAll(Arrays.asList(etcdAddresses));
      return self();
    } finally {
      this.lock.unlock();
    }
  }

  public Tarantool3Container withEtcdPrefix(String etcdPrefix) {
    try {
      this.lock.lock();
      this.etcdPrefix = etcdPrefix;
      return self();
    } finally {
      this.lock.unlock();
    }
  }

  @Override
  public Tarantool3Container withConfigPath(Path configPath) {
    try {
      this.lock.lock();
      this.configPath = configPath;
      return self();
    } finally {
      this.lock.unlock();
    }
  }

  @Override
  public Tarantool3Container withMigrationsPath(Path migrationsPath) {
    try {
      this.lock.lock();
      this.migrationsPath = migrationsPath;
      return self();
    } finally {
      this.lock.unlock();
    }
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
  public InetSocketAddress internalAddress() {
    return new InetSocketAddress(this.node, TarantoolContainer.DEFAULT_TARANTOOL_PORT);
  }

  @Override
  public void stopWithSafeMount() {
    if (this.isClosed.get()) {
      return;
    }

    try {
      this.lock.lock();
      super.stop();
    } finally {
      this.lock.unlock();
    }
  }

  @Override
  public TarantoolContainer<Tarantool3Container> withFixedExposedPort(
      int hostPort, int containerPort) {
    this.addFixedExposedPort(hostPort, containerPort);
    return this;
  }

  @Override
  protected void configure() {
    try {
      this.lock.lock();

      if (this.configured) {
        return;
      }
      this.mountDataDirectory = Utils.createTempDirectory(this.node);
      final String mountPathString = this.mountDataDirectory.toAbsolutePath().toString();

      addFileSystemBind(
          mountPathString,
          TarantoolContainer.DEFAULT_DATA_DIR.toAbsolutePath().toString(),
          BindMode.READ_WRITE,
          SelinuxContext.SHARED);

      if (this.configPath != null && Files.isRegularFile(this.configPath)) {
        final Path configPathInContainer =
            TarantoolContainer.DEFAULT_DATA_DIR.resolve(this.configPath.getFileName());

        LOGGER.info(
            "Copy tarantool configuration file from '{}' into '{}'",
            this.configPath,
            configPathInContainer);
        withCopyFileToContainer(
            MountableFile.forHostPath(this.configPath),
            configPathInContainer.toAbsolutePath().toString());
      } else {
        LOGGER.warn(
            "Path to tarantool config file is 'null', directory or doesn't exist. Passed path:"
                + " '{}'",
            this.configPath);
      }

      if (this.migrationsPath != null && Files.isRegularFile(this.migrationsPath)) {
        LOGGER.warn("Migrations path '{}' is regular file. Skipped...", this.migrationsPath);
      } else if (this.migrationsPath == null) {
        LOGGER.warn("Migrations path doesn't passed or 'null'. Skipped...");
      } else {
        final Path migrationsPathInContainer =
            TarantoolContainer.DEFAULT_DATA_DIR.resolve(this.migrationsPath.getFileName());
        LOGGER.info(
            "Copy tarantool migrations directory from '{}' into '{}'",
            this.migrationsPath,
            migrationsPathInContainer);

        withCopyFileToContainer(
            MountableFile.forHostPath(this.migrationsPath),
            migrationsPathInContainer.toAbsolutePath().toString());
      }

      addExposedPort(TarantoolContainer.DEFAULT_TARANTOOL_PORT);
      withNetworkAliases(this.node);
      withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix(this.node));
      withEnv("TT_INSTANCE_NAME", this.node);
      withEnv("TT_DATABASE_INSTANCE_UUID", this.instanceUuid.toString());
      final String DATA_DIR_STR = TarantoolContainer.DEFAULT_DATA_DIR.toAbsolutePath().toString();
      withEnv("TT_WAL_DIR", DATA_DIR_STR);
      withEnv("TT_VINYL_DIR", DATA_DIR_STR);
      withEnv("TT_SNAPSHOT_DIR", DATA_DIR_STR);
      withEnv("TT_MEMTX_DIR", DATA_DIR_STR);

      if (this.etcdAddresses.isEmpty()) {
        LOGGER.warn(
            "Tarantool will use the configuration from the local file system because no etcd"
                + " cluster addresses were passed");
        withEnv(
            "TT_CONFIG",
            TarantoolContainer.DEFAULT_DATA_DIR
                .resolve(this.configPath.getFileName())
                .toAbsolutePath()
                .toString());
      } else {
        LOGGER.warn(
            "Tarantool will use the configuration from the etcd cluster. Endpoints : {}",
            this.etcdAddresses);
        withEnv("TT_CONFIG_ETCD_ENDPOINTS", joinEtcdAddresses(this.etcdAddresses));
        withEnv("TT_CONFIG_ETCD_PREFIX", this.etcdPrefix);
      }
      withPrivilegedMode(true);
      withCreateContainerCmdModifier(
          cmd -> {
            cmd.withName(this.node).withUser("root");
            String[] originalEntrypoint =
                cmd.getEntrypoint() != null && cmd.getEntrypoint().length > 0
                    ? cmd.getEntrypoint()
                    : new String[] {"tarantool"};
            String dataDir = TarantoolContainer.DEFAULT_DATA_DIR.toAbsolutePath().toString();
            String entrypointStr = String.join(" ", originalEntrypoint);
            cmd.withEntrypoint(
                "sh",
                "-c",
                String.format(
                    "chmod -R 777 %s 2>/dev/null || true; exec %s \"$@\"", dataDir, entrypointStr));
          });
      this.configured = true;
    } catch (Exception e) {
      throw new ContainerLaunchException(e.getMessage(), e);
    } finally {
      this.lock.unlock();
    }
  }

  @Override
  protected void containerIsStarted(InspectContainerResponse containerInfo) {
    Utils.bindExposedPorts(this);
  }

  @Override
  public void start() {
    if (this.isClosed.get()) {
      throw new IllegalStateException("Container is already closed. Please create new container");
    }

    LOGGER.debug(
        "Starting Tarantool3 container[instance={}, uuid={}]", this.node, this.instanceUuid);
    super.start();
  }

  @Override
  public void stop() {
    if (this.isClosed.get()) {
      return;
    }

    try {
      this.lock.lock();
      super.stop();
      this.isClosed.set(true);
    } finally {
      this.lock.unlock();
    }
  }

  private static String joinEtcdAddresses(List<HttpHost> etcdAddresses) {
    if (etcdAddresses == null || etcdAddresses.isEmpty()) {
      return null;
    }
    return etcdAddresses.stream().map(HttpHost::toString).collect(Collectors.joining(","));
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    Tarantool3Container that = (Tarantool3Container) o;
    return Objects.equals(instanceUuid, that.instanceUuid)
        && Objects.equals(node, that.node)
        && Objects.equals(etcdAddresses, that.etcdAddresses)
        && Objects.equals(etcdPrefix, that.etcdPrefix)
        && Objects.equals(mountDataDirectory, that.mountDataDirectory)
        && Objects.equals(configPath, that.configPath)
        && Objects.equals(migrationsPath, that.migrationsPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        instanceUuid,
        node,
        etcdAddresses,
        etcdPrefix,
        mountDataDirectory,
        configPath,
        migrationsPath);
  }
}
